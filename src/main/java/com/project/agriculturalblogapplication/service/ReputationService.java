package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.ExpertBadge;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.ReputationEvent;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.ReputationReason;
import com.project.agriculturalblogapplication.enums.VoteDirection;
import com.project.agriculturalblogapplication.model.AuthorStanding;
import com.project.agriculturalblogapplication.model.TopicTotals;
import com.project.agriculturalblogapplication.model.response.ReputationResponse;
import com.project.agriculturalblogapplication.model.response.TopicReputation;
import com.project.agriculturalblogapplication.repositories.ReputationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Writes the reputation ledger as votes and accepts change. Nothing is updated in place: taking something back
 * appends the same reason with negated points, so every point can be traced to a post.
 */
// shortcut: totals are summed on every read, fine for thousands of events per user; cache per user and topic when
// profile or list reads get slow.
@Service
@RequiredArgsConstructor
public class ReputationService {

    public static final int MIN_REPUTATION_TO_DOWNVOTE = 50;

    public static final int TOP_CONTRIBUTOR_POINTS = 250;

    public static final int TOP_CONTRIBUTOR_ACCEPTED_ANSWERS = 5;

    private final ReputationEventRepository reputationEventRepository;

    private final ExpertService expertService;

    public long total(Long userId) {
        return reputationEventRepository.totalFor(userId);
    }

    /** Standings for several users in two queries, for lists; users with no points and no badge are absent (use NONE). */
    public Map<Long, AuthorStanding> standings(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<TopicTotals>> ledger = reputationEventRepository.topicTotals(userIds).stream()
                .collect(Collectors.groupingBy(TopicTotals::userId));
        Map<Long, Set<Long>> experts = expertService.verifiedTopics(userIds);
        Set<Long> ranked = new HashSet<>(ledger.keySet());
        ranked.addAll(experts.keySet());
        return ranked.stream().collect(Collectors.toMap(Function.identity(), userId -> standingOf(
                ledger.getOrDefault(userId, List.of()), experts.getOrDefault(userId, Set.of()))));
    }

    /** Every topic the user has points or a Verified Expert badge in, highest points first. */
    public ReputationResponse of(Long userId) {
        Map<Long, ExpertBadge> badges = expertService.activeBadges(userId).stream()
                .collect(Collectors.toMap(badge -> badge.getTopic().getId(), Function.identity()));
        List<TopicTotals> totals = new ArrayList<>(reputationEventRepository.topicTotals(List.of(userId)));
        Set<Long> withPoints = totals.stream().map(TopicTotals::categoryId).collect(Collectors.toSet());
        badges.values().stream().filter(badge -> !withPoints.contains(badge.getTopic().getId()))
                .forEach(badge -> totals.add(new TopicTotals(userId, badge.getTopic().getId(),
                        badge.getTopic().getCategoryName(), 0, 0)));
        AuthorStanding standing = standingOf(totals, badges.keySet());
        List<TopicReputation> topics = totals.stream()
                .sorted(Comparator.comparingLong(TopicTotals::points).reversed())
                .map(t -> new TopicReputation(t.categoryId(), t.categoryName(), t.points(), t.acceptedAnswers(),
                        standing.badgesIn(t.categoryId()),
                        t.categoryId() == null || !badges.containsKey(t.categoryId()) ? null : badges.get(t.categoryId()).getFieldTitle()))
                .toList();
        return new ReputationResponse(standing.reputation(), topics);
    }

    private static AuthorStanding standingOf(List<TopicTotals> topics, Set<Long> expertTopics) {
        return new AuthorStanding(
                topics.stream().mapToLong(TopicTotals::points).sum(),
                topics.stream().filter(ReputationService::isTopContributor).map(TopicTotals::categoryId).collect(Collectors.toSet()),
                expertTopics);
    }

    /** Computed on read, so the badge comes and goes with the points. Never for the "no topic" bucket. */
    private static boolean isTopContributor(TopicTotals totals) {
        return totals.categoryId() != null
                && totals.points() >= TOP_CONTRIBUTOR_POINTS
                && totals.acceptedAnswers() >= TOP_CONTRIBUTOR_ACCEPTED_ANSWERS;
    }

    public void questionVoteChanged(Question question, VoteDirection before, VoteDirection after) {
        voteChanged(question.getUser(), question, null, before, after, ReputationReason.QUESTION_UPVOTED);
    }

    public void answerVoteChanged(Answer answer, VoteDirection before, VoteDirection after) {
        voteChanged(answer.getUser(), answer.getQuestion(), answer, before, after, ReputationReason.ANSWER_UPVOTED);
    }

    /** +15 to the answerer and +2 to the asker; nothing for accepting your own answer. */
    public void accepted(Answer answer) {
        acceptance(answer, 1);
    }

    public void unaccepted(Answer answer) {
        acceptance(answer, -1);
    }

    private void voteChanged(User author, Question question, Answer answer, VoteDirection before, VoteDirection after,
                             ReputationReason upvoted) {
        if (before == after) {
            return;
        }
        if (before != null) {
            record(author, question, answer, before == VoteDirection.UP ? upvoted : ReputationReason.DOWNVOTED, -1);
        }
        if (after != null) {
            record(author, question, answer, after == VoteDirection.UP ? upvoted : ReputationReason.DOWNVOTED, 1);
        }
    }

    private void acceptance(Answer answer, int sign) {
        Question question = answer.getQuestion();
        if (answer.getUser().getId().equals(question.getUser().getId())) {
            return;
        }
        record(answer.getUser(), question, answer, ReputationReason.ANSWER_ACCEPTED, sign);
        record(question.getUser(), question, answer, ReputationReason.ACCEPTED_AN_ANSWER, sign);
    }

    /** {@code answer} null means the points belong to the question itself. */
    private void record(User user, Question question, Answer answer, ReputationReason reason, int sign) {
        ReputationEvent event = new ReputationEvent();
        event.setUser(user);
        event.setTopic(question.getCategory());
        if (answer == null) {
            event.setQuestion(question);
        } else {
            event.setAnswer(answer);
        }
        event.setReason(reason);
        event.setPoints(sign * reason.getPoints());
        reputationEventRepository.save(event);
    }
}
