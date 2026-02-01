package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.CreateAnswerRequest;
import com.project.agriculturalblogapplication.model.request.ReplyToAnswerRequest;
import com.project.agriculturalblogapplication.model.request.UpdateAnswerRequest;
import com.project.agriculturalblogapplication.model.response.AnswerResponse;
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

    public AnswerResponse create(CreateAnswerRequest request, String lang) {
        Question question = questionService.findByIdWithException(request.getQuestionId());

        User user = userService.findByIdWithException(request.getUserId(), lang);

        Answer answer = new Answer();
        answer.setContent(request.getContent());
        answer.setUser(user);
        answer.setQuestion(question);
        answer = answerRepository.save(answer);

        return mapToAnswerResponse(answer);
    }

    public AnswerResponse update(UpdateAnswerRequest request, String lang) {
        Answer answer = findByIdWithException(request.getAnswerId());
        answer.setContent(request.getContent());
        answer = answerRepository.save(answer);

        return mapToAnswerResponse(answer);
    }

    public void delete(Long answerId) {
        Answer answer = findByIdWithException(answerId);
        answerRepository.delete(answer);
    }

    public List<AnswerResponse> getAllByQuestionId(Long questionId) {
        List<Answer> answers = answerRepository.findAllByQuestionId(questionId);

        return answers.stream()
                .map(this::mapToAnswerResponse)
                .toList();
    }

    public AnswerResponse replyToAnswer(ReplyToAnswerRequest request, String lang) {
        Question question = questionService.findByIdWithException(request.getQuestionId());

        User user = userService.findByIdWithException(request.getUserId(), lang);

        Answer parentAnswer = findByIdWithException(request.getParentAnswerId());

        if (!parentAnswer.getQuestion().getId().equals(request.getQuestionId())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_ANSWER_QUESTION_MISMATCH);
        }

        Answer reply = new Answer();
        reply.setContent(request.getContent());
        reply.setUser(user);
        reply.setQuestion(question);
        reply.setParentAnswer(parentAnswer);
        reply = answerRepository.save(reply);

        return mapToAnswerResponse(reply);
    }

    public List<AnswerResponse> viewReplies(Long parentAnswerId) {
        Answer parentAnswer = findByIdWithException(parentAnswerId);

        return parentAnswer.getReplies().stream()
                .map(this::mapToAnswerResponse)
                .toList();
    }

    public Answer findByIdWithException(Long answerId) {
        return answerRepository.findById(answerId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_ANSWER_NOT_FOUND));
    }

    public AnswerResponse findById(Long answerId) {
        return mapToAnswerResponse(findByIdWithException(answerId));
    }


    private AnswerResponse mapToAnswerResponse(Answer answer){
        AnswerResponse response = new AnswerResponse();
        response.setAnswerId(answer.getId());
        response.setUserId(answer.getUser().getId());
        response.setQuestionId(answer.getQuestion().getId());
        response.setContent(answer.getContent());
        response.setCreatedBy(answer.getCreatedBy());
        response.setCreationDate(answer.getCreationDate());
        response.setLastModifiedBy(answer.getLastModifiedBy());
        response.setLastModifiedDate(answer.getLastModifiedDate());

        return response;
    }
}