package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.AiAnswer;
import com.project.agriculturalblogapplication.model.request.CreateAiResponseRequest;
import com.project.agriculturalblogapplication.model.response.AiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.AiSourceResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.AiRepositories;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiRepositories aiRepositories;

    private final ChatClient chatClient;

    private final AuthorizationService authorizationService;

    public AiAnswerResponse ask(String question, String lang) {
        Long userId = authorizationService.currentUserId(lang);

        ChatClientResponse response = chatClient.prompt()
                .user(question)
                .call()
                .chatClientResponse();
        String answer = response.chatResponse().getResult().getOutput().getText();

        CreateAiResponseRequest request = new CreateAiResponseRequest();
        request.setQuestion(question);
        request.setAnswer(answer);
        request.setUserId(userId);
        save(request);

        return new AiAnswerResponse(answer, sourcesOf(response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT)));
    }

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
