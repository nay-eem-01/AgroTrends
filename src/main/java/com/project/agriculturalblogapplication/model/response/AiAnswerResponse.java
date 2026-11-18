package com.project.agriculturalblogapplication.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AiAnswerResponse(
        @Schema(description = "The AI's answer")
        String answer,
        @Schema(description = "Posts whose excerpts were given to the AI for this answer, most relevant first; empty when no post matched")
        List<AiSourceResponse> sources
) {
}
