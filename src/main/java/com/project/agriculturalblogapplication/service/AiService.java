package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.AiAnswer;
import com.project.agriculturalblogapplication.model.request.CreateAiResponseRequest;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.AiRepositories;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiRepositories aiRepositories;

    public Page<AiAnswer> getAllByUserId(PaginationArgs paginationArgs, Long userId){
        Pageable pageable = CommonUtils.getPageable(paginationArgs);

        List<AiAnswer> aiAnswers = aiRepositories.findAllByUserId(userId);

        return new PageImpl<>(aiAnswers, pageable, aiAnswers.size());
    }

    public AiAnswer save(CreateAiResponseRequest request){
        AiAnswer aiAnswer = new AiAnswer();
        aiAnswer.setAiAnswer(request.getAnswer());
        aiAnswer.setQuestion(request.getQuestion());
        aiAnswer.setUserId(request.getUserId());

        return aiRepositories.save(aiAnswer);
    }
}
