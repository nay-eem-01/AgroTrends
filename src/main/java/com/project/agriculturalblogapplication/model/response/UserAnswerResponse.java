package com.project.agriculturalblogapplication.model.response;

import java.time.Instant;

/** One answer on a user's profile, with the question it answers. */
public record UserAnswerResponse(
        Long answerId,
        Long questionId,
        String questionTitle,
        String content,
        long score,
        boolean accepted,
        Instant createdAt
) {
}
