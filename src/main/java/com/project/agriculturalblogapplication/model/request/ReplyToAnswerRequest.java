package com.project.agriculturalblogapplication.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReplyToAnswerRequest {

    private Long questionId;
    private Long userId;
    private Long parentAnswerId;
    private String content;
}
