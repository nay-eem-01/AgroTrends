package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Sends a private AI question to the experts as a public question in a topic. */
@Getter
@Setter
public class AskExpertsRequest {

    @NotNull(message = ErrorCode.ERROR_CATEGORY_IS_REQUIRED)
    private Long categoryId;

    /** Optional; defaults to the start of the asked text. */
    @Size(max = 150, message = ErrorCode.ERROR_QUESTION_TITLE_TOO_LONG)
    private String title;
}
