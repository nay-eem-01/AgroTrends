package com.project.agriculturalblogapplication.events;

/**
 * The review status of a question's AI answer changed.
 * Published inside the transaction; listeners act after it commits.
 */
public record AiAnswerReviewed(Long questionId) {
}
