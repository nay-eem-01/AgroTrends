package com.project.agriculturalblogapplication.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateQuestionRequest {
    
    @NotNull
    private Long questionId;
    
    @NotBlank
    private String title;
    
    @NotBlank
    private String content;
}