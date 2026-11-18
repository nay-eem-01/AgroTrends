package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record BlogAssistRequest(
    @Schema(description = "Draft title", example = "Managing rice blast")
    @NotBlank(message = ErrorCode.ERROR_TITLE_IS_REQUIRED)
    String title,
    @Schema(description = "Draft content")
    @NotBlank(message = ErrorCode.ERROR_CONTENT_IS_REQUIRED)
    String content
) {}
