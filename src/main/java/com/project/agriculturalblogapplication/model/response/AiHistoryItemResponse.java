package com.project.agriculturalblogapplication.model.response;

import java.time.Instant;
import java.util.List;

/** {@code publishedQuestionId}: the public question this was sent to the experts as, or null. */
public record AiHistoryItemResponse(Long id, String question, String answer, List<AiSourceResponse> sources,
                                    Long publishedQuestionId, Instant askedAt) {
}
