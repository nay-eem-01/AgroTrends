package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AiProperties;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.AiAnswer;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.AiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.AiSourceResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.AiRepositories;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiRepositories aiRepositories;

    private final ChatClient chatClient;

    private final AuthorizationService authorizationService;

    private final AiProperties aiProperties;

    public AiAnswerResponse ask(String question, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        assertDailyLimitNotReached(userId, lang);

        ChatClientResponse response = callModel(question, lang);
        ChatResponse chatResponse = response.chatResponse();
        String answer = chatResponse.getResult().getOutput().getText();
        Usage usage = chatResponse.getMetadata().getUsage();

        AiAnswer aiAnswer = new AiAnswer();
        aiAnswer.setUserId(userId);
        aiAnswer.setQuestion(question);
        aiAnswer.setAiAnswer(answer);
        aiAnswer.setPromptTokens(usage.getPromptTokens());
        aiAnswer.setCompletionTokens(usage.getCompletionTokens());
        aiRepositories.save(aiAnswer);

        return new AiAnswerResponse(answer, sourcesOf(response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT)));
    }

    /** Only answered questions count, so a failed or timed-out call does not use up the allowance. */
    private void assertDailyLimitNotReached(Long userId, String lang) {
        long askedToday = aiRepositories.countByUserIdAndCreationDateGreaterThanEqual(userId, LocalDate.now().atStartOfDay());
        if (askedToday >= aiProperties.getDailyQuestionLimit()) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ERROR_AI_DAILY_LIMIT_REACHED, lang);
        }
    }

    private ChatClientResponse callModel(String question, String lang) {
        try {
            return chatClient.prompt()
                    .user(question)
                    .call()
                    .chatClientResponse();
        } catch (RuntimeException e) {
            // Timeouts, quota errors and outages at Gemini: the caller gets a clean 503, the log keeps the cause.
            log.error("AI call failed", e);
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.ERROR_AI_UNAVAILABLE, lang);
        }
    }

    public Page<AiAnswer> getAllByUserId(PaginationArgs paginationArgs, Long userId){
        Pageable pageable = CommonUtils.getPageable(paginationArgs);

        List<AiAnswer> aiAnswers = aiRepositories.findAllByUserId(userId);

        return new PageImpl<>(aiAnswers, pageable, aiAnswers.size());
    }

    /** One source per post, in retrieval order: several chunks of the same post collapse into one citation. */
    static List<AiSourceResponse> sourcesOf(Object retrieved) {
        if (!(retrieved instanceof List<?> documents)) {
            return List.of();
        }
        Map<Long, AiSourceResponse> sources = new LinkedHashMap<>();
        for (Object item : documents) {
            if (item instanceof Document document
                    && document.getMetadata().get(DocumentService.BLOG_ID) instanceof Number blogId) {
                sources.putIfAbsent(blogId.longValue(), new AiSourceResponse(blogId.longValue(),
                        (String) document.getMetadata().get(DocumentService.TITLE)));
            }
        }
        return List.copyOf(sources.values());
    }
}
