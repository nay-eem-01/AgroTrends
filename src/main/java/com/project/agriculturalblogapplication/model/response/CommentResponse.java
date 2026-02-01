package com.project.agriculturalblogapplication.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.agriculturalblogapplication.constatnt.AppTables;
import com.project.agriculturalblogapplication.entities.Comment;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

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
    private String createdBy;
    private String lastModifiedBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime creationDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    protected LocalDateTime lastModifiedDate;
}
