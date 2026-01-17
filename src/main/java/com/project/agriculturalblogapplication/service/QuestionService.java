package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateQuestionRequest;
import com.project.agriculturalblogapplication.model.request.UpdateQuestionRequest;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final UserService userService;

    public Question create(CreateQuestionRequest request, String lang) {
        User user = userService.findByIdWithException(request.getUserId(), lang);

        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setUser(user);

        return questionRepository.save(question);
    }

    public Page<Question> getAll(PaginationArgs paginationArgs) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);
        return questionRepository.findAll(pageable);
    }

    public Page<Question> getAllByUser(PaginationArgs paginationArgs, Long userId, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);

        User user = userService.findByIdWithException(userId, lang);
        List<Question> questions = questionRepository.findAllByUser(user);

        return new PageImpl<>(questions, pageable, questions.size());
    }

    public Question update(UpdateQuestionRequest request) {
        Question question = findByIdWithException(request.getQuestionId());

        question.setTitle(request.getTitle());
        question.setContent(request.getContent());

        return questionRepository.save(question);
    }

    public void delete(Long id) {
        Question question = findByIdWithException(id);
        questionRepository.delete(question);
    }

    public Question findByIdWithException(Long questionId) {
        return questionRepository.findById(questionId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_QUESTION_NOT_FOUND));
    }
}