package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Comment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CommentResponse {
    private Long userId;
    private Long blogId;
    private Long parentCommentId;
    private Long commentId;
    private String content;

    /** The author's display name (never their e-mail). */
    private String authorName;

    private Instant createdAt;

    private Instant updatedAt;
}
