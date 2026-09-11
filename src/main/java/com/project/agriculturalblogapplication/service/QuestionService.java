package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateQuestionRequest;
import com.project.agriculturalblogapplication.model.request.UpdateQuestionRequest;
import com.project.agriculturalblogapplication.model.response.QuestionResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    private final UserService userService;

    public QuestionResponse create(CreateQuestionRequest request, String lang) {
        User user = userService.findByIdWithException(request.getUserId(), lang);

        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setUser(user);
        question = questionRepository.save(question);

        return mapToQuestionResponse(question);
    }

    public Page<QuestionResponse> getAll(PaginationArgs paginationArgs) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);
        Page<Question> questions = questionRepository.findAll(pageable);
        return questions.map(this::mapToQuestionResponse);
    }

    public Page<QuestionResponse> getAllByUser(PaginationArgs paginationArgs, Long userId, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);
        User user = userService.findByIdWithException(userId, lang);
        Page<Question> questions = questionRepository.findAllByUser(user, pageable);
        return questions.map(this::mapToQuestionResponse);
    }

    public QuestionResponse update(UpdateQuestionRequest request, String lang) {
        Question question = findByIdWithException(request.getQuestionId());

        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question = questionRepository.save(question);

        return mapToQuestionResponse(question);
    }

    public void delete(Long id, String lang) {
        Question question = findByIdWithException(id);
        questionRepository.delete(question);
    }

    public Question findByIdWithException(Long questionId) {
        return questionRepository.findById(questionId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_QUESTION_NOT_FOUND));
    }

    public QuestionResponse findById(Long questionId) {
        return mapToQuestionResponse(findByIdWithException(questionId));
    }

    private QuestionResponse mapToQuestionResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setQuestionId(question.getId());
        response.setUserId(question.getUser().getId());
        response.setTitle(question.getTitle());
        response.setContent(question.getContent());
        response.setCreatedAt(question.getCreationDate());
        response.setUpdatedAt(question.getLastModifiedDate());

        return response;
    }
}
