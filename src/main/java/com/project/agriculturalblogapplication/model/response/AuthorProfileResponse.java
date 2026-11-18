package com.project.agriculturalblogapplication.model.response;

import java.util.List;

/**
 * A user's public profile, their portfolio: who they are, where they are a verified expert and how active they are.
 * {@code userId} is for the user-keyed calls (reputation, questions, answers). No e-mail, phone or roles.
 */
public record AuthorProfileResponse(
        Long authorId,
        Long userId,
        String name,
        String designation,
        String occupation,
        String workPlaceOrInstitution,
        List<String> specialities,
        String bio,
        String profileImageUrl,
        List<String> links,
        List<ExpertTopic> expertTopics,
        long publishedPostCount,
        long answerCount,
        long acceptedAnswerCount,
        long followerCount,
        boolean followedByMe
) {

    /** A topic the user is a verified expert in, with the field the badge was granted for. */
    public record ExpertTopic(Long topicId, String topicName, String fieldTitle) {
    }
}
