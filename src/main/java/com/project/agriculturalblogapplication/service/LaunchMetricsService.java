package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.model.response.LaunchWeekResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The numbers the week-8 go / stop rests on (PLAN §6), from the existing tables. Six queries cover the whole range;
 * the weeks are bucketed here. Hidden questions are left out; deleted ones are already gone.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LaunchMetricsService {

    static final Duration HUMAN_ANSWER_WINDOW = Duration.ofHours(24);
    static final Duration AI_REVIEW_WINDOW = Duration.ofHours(48);

    private final EntityManager entityManager;

    /** The last {@code weeks} weeks, newest first; the current one is partial. */
    public List<LaunchWeekResponse> weeks(int weeks) {
        int count = Math.max(1, Math.min(weeks, 52));
        LocalDate thisMonday = LocalDate.now(AppConstants.APP_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate firstMonday = thisMonday.minusWeeks(count - 1L);
        LocalDateTime from = serverTime(firstMonday);
        LocalDateTime now = LocalDateTime.now();

        Facts facts = new Facts(
                list("select q.id, q.user.id, q.creationDate from Question q"
                        + " where q.creationDate >= :from and q.hidden = false", from),
                list("select a.question.id, a.user.id, a.creationDate from Answer a"
                        + " where a.parentAnswer is null and a.user.id <> a.question.user.id and a.question.creationDate >= :from", from),
                list("select a.user.id, a.creationDate from Answer a where a.creationDate >= :from", from),
                list("select a.question.user.id, a.creationDate, a.status, a.reviewedAt, r.id"
                        + " from QuestionAiAnswer a left join a.reviewedBy r where a.creationDate >= :from and a.question.hidden = false", from),
                entityManager.createQuery("select b.user.id, b.creationDate, b.revokedAt from ExpertBadge b", Object[].class).getResultList(),
                list("select u.id, u.creationDate from User u where u.creationDate >= :from", from),
                new HashSet<>(entityManager.createQuery(
                        "select distinct u.id from User u join u.roles r where r.roleType in :types", Long.class)
                        .setParameter("types", List.of(RoleType.ADMIN, RoleType.SUPER_ADMIN)).getResultList()));

        List<LaunchWeekResponse> rows = new ArrayList<>();
        for (LocalDate monday = thisMonday; !monday.isBefore(firstMonday); monday = monday.minusWeeks(1)) {
            LocalDateTime start = serverTime(monday);
            LocalDateTime end = serverTime(monday.plusWeeks(1));
            boolean partial = end.isAfter(now);
            LaunchWeekResponse staff = facts.week(monday, partial, start, end, true, null);
            rows.add(facts.week(monday, partial, start, end, false, staff));
        }
        return rows;
    }

    /** Midnight in Bangladesh as the server's local time, which is how creation dates are stored. */
    private static LocalDateTime serverTime(LocalDate day) {
        return day.atStartOfDay(AppConstants.APP_ZONE).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private List<Object[]> list(String jpql, LocalDateTime from) {
        return entityManager.createQuery(jpql, Object[].class).setParameter("from", from).getResultList();
    }

    record Facts(List<Object[]> questions, List<Object[]> humanAnswers, List<Object[]> answerActivity,
                         List<Object[]> aiAnswers, List<Object[]> badges, List<Object[]> signUps, Set<Long> staff) {

        /** One week's row, counting everyone's activity or only staff's. */
        LaunchWeekResponse week(LocalDate monday, boolean partial, LocalDateTime start, LocalDateTime end,
                                boolean staffOnly, LaunchWeekResponse staffActivity) {
            Predicate<Long> by = staffOnly ? staff::contains : userId -> true;
            Predicate<LocalDateTime> inWeek = time -> !time.isBefore(start) && time.isBefore(end);

            Set<Long> askers = new HashSet<>();
            Map<Long, LocalDateTime> askedAt = new HashMap<>();
            for (Object[] q : questions) {
                if (inWeek.test((LocalDateTime) q[2])) {
                    askedAt.put((Long) q[0], (LocalDateTime) q[2]);
                    if (by.test((Long) q[1])) {
                        askers.add((Long) q[1]);
                    }
                }
            }
            Set<Long> answerers = new HashSet<>();
            for (Object[] a : answerActivity) {
                if (inWeek.test((LocalDateTime) a[1]) && by.test((Long) a[0])) {
                    answerers.add((Long) a[0]);
                }
            }
            Set<Long> members = new HashSet<>(askers);
            members.addAll(answerers);

            // Everyone: questions asked by anyone, answered by anyone. Staff: questions staff asked, and questions
            // whose answer within 24 hours came from staff.
            long questionCount = !staffOnly ? askedAt.size() : questions.stream()
                    .filter(q -> askedAt.containsKey((Long) q[0]) && by.test((Long) q[1])).count();
            Set<Long> answeredInTime = new HashSet<>();
            for (Object[] a : humanAnswers) {
                LocalDateTime asked = askedAt.get((Long) a[0]);
                if (asked != null && by.test((Long) a[1])
                        && !((LocalDateTime) a[2]).isAfter(asked.plus(HUMAN_ANSWER_WINDOW))) {
                    answeredInTime.add((Long) a[0]);
                }
            }

            long ai = 0;
            long reviewedInTime = 0;
            for (Object[] a : aiAnswers) {
                LocalDateTime created = (LocalDateTime) a[1];
                if (!inWeek.test(created)) {
                    continue;
                }
                if (by.test((Long) a[0])) {
                    ai++;
                }
                LocalDateTime reviewedAt = (LocalDateTime) a[3];
                boolean checked = a[2] == AiAnswerStatus.CONFIRMED || a[2] == AiAnswerStatus.CORRECTED;
                if (checked && reviewedAt != null && !reviewedAt.isAfter(created.plus(AI_REVIEW_WINDOW))
                        && (!staffOnly || (a[4] != null && by.test((Long) a[4])))) {
                    reviewedInTime++;
                }
            }

            Set<Long> experts = new HashSet<>();
            for (Object[] b : badges) {
                LocalDateTime revoked = (LocalDateTime) b[2];
                if (((LocalDateTime) b[1]).isBefore(end) && (revoked == null || !revoked.isBefore(end)) && by.test((Long) b[0])) {
                    experts.add((Long) b[0]);
                }
            }
            long newMembers = signUps.stream()
                    .filter(u -> inWeek.test((LocalDateTime) u[1]) && by.test((Long) u[0])).count();

            return new LaunchWeekResponse(monday, partial, askers.size(), answerers.size(), members.size(), questionCount,
                    answeredInTime.size(), ai, reviewedInTime, experts.size(), newMembers, staffActivity);
        }
    }
}
