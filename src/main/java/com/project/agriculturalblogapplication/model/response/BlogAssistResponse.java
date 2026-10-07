package com.project.agriculturalblogapplication.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record BlogAssistResponse(
        @Schema(description = "A summary of at most three sentences, in the draft's language")
        String summary,
        @Schema(description = "Three to six short, lowercase topic tags")
        List<String> suggestedTags
) {
}
