package com.project.agriculturalblogapplication.model.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateCommentRequest {

    private Long userId;

    private String content;

    private Long blogId;
}
