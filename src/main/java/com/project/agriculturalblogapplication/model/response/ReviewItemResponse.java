package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;

/** One AI answer waiting for an expert, with enough of its question to judge it. */
public record ReviewItemResponse(Long questionId, String questionTitle, String questionContent, CategoryResponse topic,
                                 QuestionAiAnswerResponse aiAnswer) {

    public static ReviewItemResponse from(QuestionAiAnswer aiAnswer) {
        Question question = aiAnswer.getQuestion();
        return new ReviewItemResponse(question.getId(), question.getTitle(), question.getContent(),
                CategoryResponse.from(question.getCategory()), QuestionAiAnswerResponse.from(aiAnswer));
    }
}
