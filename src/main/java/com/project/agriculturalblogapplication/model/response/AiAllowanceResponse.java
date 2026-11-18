package com.project.agriculturalblogapplication.model.response;

import java.time.OffsetDateTime;

/** The advisor's daily allowance for the caller; it resets at midnight in Bangladesh. */
public record AiAllowanceResponse(int limit, long usedToday, OffsetDateTime resetsAt) {
}
