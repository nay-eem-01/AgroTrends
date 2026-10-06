package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateQuestionRequest;
import com.project.agriculturalblogapplication.model.request.UpdateQuestionRequest;
import com.project.agriculturalblogapplication.model.response.QuestionResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.AgriInfo;
import com.project.agriculturalblogapplication.repositories.AgriSpecifications;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.util.Set;
@Service
@RequiredArgsConstructor
public class QuestionService {

    static final Set<String> SORTABLE_FIELDS = Set.of("creationDate", "lastModifiedDate", "title");

    private final QuestionRepository questionRepository;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    public QuestionResponse create(CreateQuestionRequest request, String lang) {
        User user = userService.findByIdWithException(authorizationService.currentUserId(lang), lang);

        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setUser(user);
        if (request.getAgri() != null) {
            question.setAgri(request.getAgri().toMetadata());
        }
        question = questionRepository.save(question);

        return mapToQuestionResponse(question);
    }

    /** All questions, optionally filtered by crop, season, region and soil. */
    public Page<QuestionResponse> getAll(PaginationArgs paginationArgs, AgriInfo filter, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs, SORTABLE_FIELDS, lang);
        Page<Question> questions = questionRepository.findAll(AgriSpecifications.matches(filter), pageable);
        return questions.map(this::mapToQuestionResponse);
    }

    public Page<QuestionResponse> getAllByUser(PaginationArgs paginationArgs, Long userId, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs, SORTABLE_FIELDS, lang);
        User user = userService.findByIdWithException(userId, lang);
        Page<Question> questions = questionRepository.findAllByUser(user, pageable);
        return questions.map(this::mapToQuestionResponse);
    }

    public QuestionResponse update(UpdateQuestionRequest request, String lang) {
        Question question = findByIdWithException(request.getQuestionId());
        authorizationService.assertOwnerOrAdmin(question.getUser().getId(), lang);

        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        // Omitted agri info keeps the current values.
        if (request.getAgri() != null) {
            question.setAgri(request.getAgri().toMetadata());
        }
        question = questionRepository.save(question);

        return mapToQuestionResponse(question);
    }

    public void delete(Long id, String lang) {
        Question question = findByIdWithException(id);
        authorizationService.assertOwnerOrAdmin(question.getUser().getId(), lang);
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
        response.setAuthorName(question.getUser().getName());
        response.setAgri(AgriInfo.from(question.getAgri()));
        response.setCreatedAt(CommonUtils.toInstant(question.getCreationDate()));
        response.setUpdatedAt(CommonUtils.toInstant(question.getLastModifiedDate()));

        return response;
    }
}
