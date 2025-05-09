package com.project.agriculturalblogapplication.DTOS;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerDto {
    private Long id;
    private String content;
    private Long userId;
    private Long questionId;
    private Long parentAnswerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AnswerDto> replies;
}
