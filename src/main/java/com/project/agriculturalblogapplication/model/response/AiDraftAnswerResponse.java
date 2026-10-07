package com.project.agriculturalblogapplication.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AiDraftAnswerResponse(
        @Schema(description = "Show this next to the draft: it is AI-generated and not reviewed")
        String label,
        String answer,
        List<AiSourceResponse> sources
) {
}
