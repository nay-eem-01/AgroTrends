package com.project.agriculturalblogapplication.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Why points were earned, and how many (docs/PLAN.md section 5). A reversal is the same reason with the points negated. */
@Getter
@RequiredArgsConstructor
public enum ReputationReason {
    QUESTION_UPVOTED(5),
    ANSWER_UPVOTED(10),
    DOWNVOTED(-2),
    ANSWER_ACCEPTED(15),
    ACCEPTED_AN_ANSWER(2);

    private final int points;
}
