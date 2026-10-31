package com.project.agriculturalblogapplication.model.response;

import java.time.Instant;
import java.util.List;

/**
 * A user's public summary, reached from the {@code userId} on questions and answers. {@code authorId} is null until
 * the user has an author profile (headline, bio, follow: {@code /api/authors/{authorId}}). No e-mail, phone or roles.
 */
public record UserProfileResponse(
        Long userId,
        String name,
        Long authorId,
        String location,
        String coverUrl,
        Instant joinedAt,
        long answerCount,
        long acceptedAnswerCount,
        long questionCount,
        long storyCount,
        List<Credential> credentials
) {

    /** A credential an admin checked when approving the user's expert application. */
    public record Credential(String title, String institution) {
    }
}
