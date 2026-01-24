package com.project.agriculturalblogapplication.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCommentRequest {

    private Long commentId;

    private Long userId;

    private String content;

    private Long blogId;
}
