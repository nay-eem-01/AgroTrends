package com.project.agriculturalblogapplication.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReplyToAnswerRequest {

    @NotNull

    private Long questionId;
    @NotNull
    private Long parentAnswerId;
    @NotBlank
    private String content;
}
