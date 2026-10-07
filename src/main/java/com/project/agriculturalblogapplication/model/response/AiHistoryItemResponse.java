package com.project.agriculturalblogapplication.model.response;

import java.time.Instant;

public record AiHistoryItemResponse(Long id, String question, String answer, Instant askedAt) {
}
