package com.project.agriculturalblogapplication.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuestionResponse {
    private Long questionId;
    private Long userId;
    private List<AnswerResponse> answerResponses = new ArrayList<>();
}
