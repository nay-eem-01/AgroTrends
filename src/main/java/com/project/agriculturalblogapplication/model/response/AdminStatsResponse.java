package com.project.agriculturalblogapplication.model.response;

/**
 * Overview numbers for the admin dashboard. Totals include hidden posts; "new" means the last 7 days.
 * {@code uncheckedAiAnswers} counts AI answers on visible questions still waiting for an expert.
 */
public record AdminStatsResponse(
        long users,
        long newUsers,
        long bannedUsers,
        long questions,
        long newQuestions,
        long answers,
        long publishedStories,
        long openReports,
        long pendingExpertApplications,
        long uncheckedAiAnswers
) {
}
