package com.project.agriculturalblogapplication.model.response;

import java.util.List;

/**
 * One user for the admin dashboard: {@link AdminUserResponse} plus what they posted (hidden posts included), the open
 * reports about the account itself, and the topics they are a verified expert in.
 */
public record AdminUserDetailResponse(
        AdminUserResponse user,
        long questions,
        long answers,
        long publishedStories,
        long openReports,
        List<CategoryRef> expertTopics
) {

    public record CategoryRef(Long id, String name) {
    }
}
