package com.project.agriculturalblogapplication.model.response;

/** Headline numbers for the sign-in panel; visible content only, banned members left out. */
public record PublicStatsResponse(long members, long verifiedExperts, long questions, long answeredQuestions) {
}
