package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskQuestionRequest(
    @Schema(description = "Question to ask (at most 1000 characters)", example = "ধানের রোগ সম্পর্কে বলুন")
    @NotBlank(message = ErrorCode.ERROR_QUESTION_IS_REQUIRED)
    @Size(max = AppConstants.AI_MAX_QUESTION_LENGTH, message = ErrorCode.ERROR_QUESTION_TOO_LONG)
    String question
) {}
