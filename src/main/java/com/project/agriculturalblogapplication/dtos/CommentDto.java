package com.project.agriculturalblogapplication.dtos;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {

    private Long commentId;
    private String commentContent;
    private Long userId;
    private Long blogId;
    private Long parentCommentId; // Null if top-level
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Recursive replies
    private List<CommentDto> replies = new ArrayList<>();

}
