package com.project.agriculturalblogapplication.events;

/**
 * An answer was created, edited, accepted, unaccepted or deleted; its place in the AI index may change.
 * Published inside the transaction; listeners act after it commits.
 */
public record AnswerChanged(Long answerId) {
}
