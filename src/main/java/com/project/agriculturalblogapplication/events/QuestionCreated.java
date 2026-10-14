package com.project.agriculturalblogapplication.events;

/** Published inside the transaction that saves a new question; listeners act after it commits. */
public record QuestionCreated(Long questionId) {
}
