package com.project.agriculturalblogapplication.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReplyCommentRequest {

    @NotNull

    private Long blogId;

    @NotNull

    private Long parentCommentId;

    @NotBlank

    private String content;
}
