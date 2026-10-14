package com.project.agriculturalblogapplication.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** An expert's corrected answer, posted as their own answer on the question. */
@Getter
@Setter
public class CorrectAiAnswerRequest {

    @NotBlank
    private String content;
}
