package com.project.agriculturalblogapplication.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReplyCommentRequest {

    private Long blogId;

    private Long userId;

    private Long parentCommentId;

    private String content;
}
