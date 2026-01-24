package com.project.agriculturalblogapplication.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateAnswerRequest {

    private Long answerId;
    private Long userId;
    private String content;
}
