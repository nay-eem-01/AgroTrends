package com.project.agriculturalblogapplication.events;

/** A question and everything under it is gone. Published inside the transaction; listeners act after it commits. */
public record QuestionDeleted(Long questionId) {
}
