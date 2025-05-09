package com.project.agriculturalblogapplication.DTOS;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDto {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private LocalDateTime createdAt;
    private List<AnswerDto> answers;
}
