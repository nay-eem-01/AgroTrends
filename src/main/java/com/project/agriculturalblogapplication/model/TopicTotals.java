package com.project.agriculturalblogapplication.model;

/** One user's ledger summed for one topic ({@code categoryId} null: questions asked before topics existed). */
public record TopicTotals(Long userId, Long categoryId, String categoryName, long points, long acceptedAnswers) {
}
