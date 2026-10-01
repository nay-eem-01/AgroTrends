package com.project.agriculturalblogapplication.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record AskQuestionRequest(
    @Schema(description = "Question to ask", example = "ধানের রোগ সম্পর্কে বলুন")
    String question
) {}