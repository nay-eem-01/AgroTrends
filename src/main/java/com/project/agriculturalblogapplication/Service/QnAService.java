package com.project.agriculturalblogapplication.Service;

import com.project.agriculturalblogapplication.DTOS.AnswerDto;
import com.project.agriculturalblogapplication.DTOS.QuestionDto;
import org.springframework.stereotype.Service;

import java.util.List;


public interface QnAService {
    QuestionDto postQuestion(Long userId, QuestionDto questionDto);
    AnswerDto postAnswer(Long userId, Long questionId, AnswerDto answerDto);
    AnswerDto replyToAnswer(Long userId, Long questionId, Long parentAnswerId, AnswerDto replyDto);
    List<QuestionDto> getAllQuestions();
    QuestionDto getQuestionById(Long questionId);

    QuestionDto updateQuestion(Long userId, Long questionId, QuestionDto questionDto);
    void deleteQuestion(Long userId, Long questionId);

    AnswerDto updateAnswer(Long userId, Long answerId, AnswerDto answerDto);
    void deleteAnswer(Long userId, Long answerId);

}
