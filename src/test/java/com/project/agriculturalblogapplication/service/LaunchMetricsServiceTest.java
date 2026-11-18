package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.model.response.LaunchWeekResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The definitions the go / stop rests on (frontend request B6). */
class LaunchMetricsServiceTest {

    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 11, 2, 0, 0);
    private static final LocalDateTime NEXT_MONDAY = MONDAY.plusWeeks(1);
    private static final long ASKER = 1L, OTHER_ASKER = 2L, ANSWERER = 3L, STAFF = 9L;

    private static Object[] row(Object... values) {
        return values;
    }

    private final LaunchMetricsService.Facts facts = new LaunchMetricsService.Facts(
            List.of(row(10L, ASKER, MONDAY.plusHours(1)), row(11L, OTHER_ASKER, MONDAY.plusHours(2)),
                    row(12L, STAFF, MONDAY.plusDays(2)), row(13L, ASKER, MONDAY.minusDays(1))),
            // Answers by someone other than the asker (the query leaves self-answers out).
            List.of(row(10L, ANSWERER, MONDAY.plusHours(20)),          // within 24h
                    row(11L, ANSWERER, MONDAY.plusHours(30)),          // too late...
                    row(11L, STAFF, MONDAY.plusHours(10))),            // ...but staff answered it in time
            List.of(row(ANSWERER, MONDAY.plusHours(20)), row(ANSWERER, MONDAY.plusHours(30)), row(STAFF, MONDAY.plusDays(3))),
            List.of(row(ASKER, MONDAY.plusHours(1), AiAnswerStatus.CONFIRMED, MONDAY.plusHours(40), STAFF),
                    row(OTHER_ASKER, MONDAY.plusHours(2), AiAnswerStatus.CORRECTED, MONDAY.plusHours(60), ANSWERER),
                    row(STAFF, MONDAY.plusDays(2), AiAnswerStatus.FLAGGED, MONDAY.plusDays(2).plusHours(1), STAFF)),
            List.of(row(ANSWERER, MONDAY.minusWeeks(2), null),
                    row(STAFF, MONDAY.minusWeeks(2), MONDAY.plusDays(1)),           // revoked during the week
                    row(OTHER_ASKER, NEXT_MONDAY.plusDays(1), null)),               // granted after it
            List.of(row(ASKER, MONDAY.plusHours(1)), row(STAFF, MONDAY.minusWeeks(3))),
            Set.of(STAFF));

    @Test
    void aWeekCountsTheLaunchTestDefinitions() {
        LaunchWeekResponse week = facts.week(LocalDate.of(2026, 11, 2), false, MONDAY, NEXT_MONDAY, false, null);

        assertEquals(3, week.activeAskers());
        assertEquals(2, week.activeAnswerers());
        assertEquals(4, week.activeMembers());
        assertEquals(3, week.questions());               // last Sunday's question is not this week's
        assertEquals(2, week.humanAnsweredWithin24h());  // 10 at 20 h, 11 by staff at 10 h; 12 never answered
        assertEquals(3, week.aiAnswers());
        assertEquals(1, week.aiReviewedWithin48h());     // confirmed at 40 h; corrected at 60 h is late; flagged never counts
        assertEquals(1, week.verifiedExperts());         // the revoked badge does not reach the week's end
        assertEquals(1, week.newMembers());
    }

    @Test
    void theStaffRowCountsOnlyWhatStaffDid() {
        LaunchWeekResponse staff = facts.week(LocalDate.of(2026, 11, 2), false, MONDAY, NEXT_MONDAY, true, null);

        assertEquals(1, staff.activeAskers());
        assertEquals(1, staff.activeAnswerers());
        assertEquals(1, staff.questions());
        assertEquals(1, staff.humanAnsweredWithin24h()); // 11, answered by staff
        assertEquals(1, staff.aiAnswers());
        assertEquals(1, staff.aiReviewedWithin48h());   // the confirm within 48 h was done by staff
        assertEquals(0, staff.verifiedExperts());
        assertEquals(0, staff.newMembers());
    }
}
