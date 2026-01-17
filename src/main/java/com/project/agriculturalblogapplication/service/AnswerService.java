package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final QuestionService questionService;
    private final UserService userService;
    private final AnswerRepository answerRepository;

    public Answer addNewAnswer(String content, Long userId, Long questionId, String lang) {
        Question question = questionService.findByIdWithException(questionId);
        User user = userService.findByIdWithException(userId, lang);

        Answer newAnswer = new Answer();
        newAnswer.setContent(content);
        newAnswer.setUser(user);
        newAnswer.setQuestion(question);

        return answerRepository.save(newAnswer);
    }

    public Answer updateAnswer(String content, Long answerId) {
        Answer answer = findByIdWithException(answerId);
        answer.setContent(content);

        return answerRepository.save(answer);
    }

    public void delete(Long answerId) {
        Answer answer = findByIdWithException(answerId);
        answerRepository.delete(answer);
    }

    public List<Answer> viewAllAnswersByQuestionId(Long questionId) {
        questionService.findByIdWithException(questionId);
        return answerRepository.findByQuestionIdAndParentAnswerIsNull(questionId);
    }

    public Answer replyToAnswer(String content, Long userId, Long questionId, Long parentAnswerId, String lang) {
        Question question = questionService.findByIdWithException(questionId);
        User user = userService.findByIdWithException(userId, lang);
        Answer parentAnswer = findByIdWithException(parentAnswerId);

        if (!parentAnswer.getQuestion().getId().equals(questionId)) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_ANSWER_QUESTION_MISMATCH);
        }

        Answer reply = new Answer();
        reply.setContent(content);
        reply.setUser(user);
        reply.setQuestion(question);
        reply.setParentAnswer(parentAnswer);

        return answerRepository.save(reply);
    }

    public List<Answer> viewReplies(Long parentAnswerId) {
        Answer parentAnswer = findByIdWithException(parentAnswerId);
        return parentAnswer.getReplies();
    }

    public Answer findByIdWithException(Long answerId) {
        return answerRepository.findById(answerId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_ANSWER_NOT_FOUND));
    }
}