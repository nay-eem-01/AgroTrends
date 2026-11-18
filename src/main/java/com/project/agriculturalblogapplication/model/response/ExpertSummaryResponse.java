package com.project.agriculturalblogapplication.model.response;

import java.util.List;

/** A verified expert to follow: who they are, their field and the topics they hold a badge in. */
public record ExpertSummaryResponse(
        Long userId,
        String name,
        /** The same as {@code userId}: an author is a user. */
        Long authorId,
        String avatarUrl,
        String expertField,
        String institution,
        List<Topic> topics,
        boolean followedByMe
) {

    public record Topic(Long id, String categoryName) {
    }
}
