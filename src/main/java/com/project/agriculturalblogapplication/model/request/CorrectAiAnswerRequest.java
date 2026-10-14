package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** An expert's corrected answer, posted as their own answer on the question. */
@Getter
@Setter
public class CorrectAiAnswerRequest {

    @NotBlank(message = ErrorCode.ERROR_CORRECTION_REQUIRED)
    @Size(max = 5000, message = ErrorCode.ERROR_CORRECTION_TOO_LONG)
    private String content;
}
