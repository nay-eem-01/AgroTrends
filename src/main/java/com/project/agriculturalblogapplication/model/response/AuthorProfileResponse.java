package com.project.agriculturalblogapplication.model.response;

import java.util.List;

/** An author's public page: who they are and how active they are. No e-mail, phone or roles. */
public record AuthorProfileResponse(
        Long authorId,
        String name,
        String designation,
        String occupation,
        String workPlaceOrInstitution,
        List<String> specialities,
        String bio,
        String profileImageUrl,
        long publishedPostCount,
        long followerCount,
        boolean followedByMe
) {
}
