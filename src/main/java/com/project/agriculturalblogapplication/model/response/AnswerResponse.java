package com.project.agriculturalblogapplication.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AnswerResponse {
    private Long answerId;
    private Long questionId;
    private Long userId;
    private String content;
}
