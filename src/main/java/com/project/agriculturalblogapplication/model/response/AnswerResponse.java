package com.project.agriculturalblogapplication.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AnswerResponse {
    private Long answerId;
    private Long questionId;
    private Long userId;
    private String content;

    /** The author's display name (never their e-mail). */
    private String authorName;

    private Instant createdAt;

    private Instant updatedAt;
}
