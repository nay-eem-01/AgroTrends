package com.project.agriculturalblogapplication.events;

/** A question's title, text or topic was edited. Published inside the transaction; listeners act after it commits. */
public record QuestionChanged(Long questionId) {
}
