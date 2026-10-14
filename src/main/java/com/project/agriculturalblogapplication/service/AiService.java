package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AiProperties;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.AiAnswer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.events.QuestionCreated;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.AiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.AiDraftAnswerResponse;
import com.project.agriculturalblogapplication.model.response.AiHistoryItemResponse;
import com.project.agriculturalblogapplication.model.response.AiSourceResponse;
import com.project.agriculturalblogapplication.model.response.QuestionAiAnswerResponse;
import com.project.agriculturalblogapplication.repositories.AiRepositories;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.util.SafeHtml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    static final String AI_DRAFT_LABEL = "AI draft - not reviewed by an expert";

    private final AiRepositories aiRepositories;

    private final ChatClient chatClient;

    private final AuthorizationService authorizationService;

    private final AiProperties aiProperties;

    private final QuestionService questionService;

    private final AnswerService answerService;

    private final QuestionAiAnswerRepository questionAiAnswerRepository;

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

    /**
     * An AI answer for a community question nobody has answered yet. It goes through {@link #ask}, so it counts
     * against the caller's daily limit and appears in their history; it is returned, not posted as an answer.
     */
    public AiDraftAnswerResponse draftAnswer(Long questionId, String lang) {
        Question question = questionService.findByIdWithException(questionId);
        if (answerService.hasAnswers(questionId)) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_QUESTION_ALREADY_ANSWERED, lang);
        }
        String text = question.getTitle() + "\n\n" + question.getContent();
        AiAnswerResponse answer = ask(text.length() > AppConstants.AI_MAX_QUESTION_LENGTH
                ? text.substring(0, AppConstants.AI_MAX_QUESTION_LENGTH) : text, lang);
        return new AiDraftAnswerResponse(AI_DRAFT_LABEL, answer.answer(), answer.sources());
    }

    /**
     * Answers a new community question once it is committed, off the request thread. Never throws: a failure or
     * the asker's daily limit just leaves the question without an AI answer.
     */
    // shortcut: a failed generation is not retried; add a retry sweep if Gemini outages leave many questions bare.
    @Async
    @TransactionalEventListener
    public void answerQuestion(QuestionCreated event) {
        try {
            Question question = questionService.findByIdWithException(event.questionId());
            long answeredToday = questionAiAnswerRepository.countByQuestionUserIdAndCreationDateGreaterThanEqual(
                    question.getUser().getId(), LocalDate.now().atStartOfDay());
            if (answeredToday >= aiProperties.getDailyQuestionLimit()) {
                log.info("No AI answer for question {}: the asker reached today's limit", question.getId());
                return;
            }
            ChatClientResponse response = chatClient.prompt().user(promptFor(question)).call().chatClientResponse();
            ChatResponse chatResponse = response.chatResponse();
            Usage usage = chatResponse.getMetadata().getUsage();

            QuestionAiAnswer aiAnswer = new QuestionAiAnswer();
            aiAnswer.setQuestion(question);
            aiAnswer.setAnswer(chatResponse.getResult().getOutput().getText());
            aiAnswer.setSources(sourcesOf(response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT)));
            aiAnswer.setPromptTokens(usage.getPromptTokens());
            aiAnswer.setCompletionTokens(usage.getCompletionTokens());
            questionAiAnswerRepository.save(aiAnswer);
        } catch (RuntimeException e) {
            log.error("AI answer for question {} failed", event.questionId(), e);
        }
    }

    /** Title and plain text, capped like a private question so one long post cannot run up the bill. */
    static String promptFor(Question question) {
        String text = question.getTitle() + "\n\n" + SafeHtml.plainText(question.getContent());
        return text.length() > AppConstants.AI_MAX_QUESTION_LENGTH ? text.substring(0, AppConstants.AI_MAX_QUESTION_LENGTH) : text;
    }

    /** The question's AI answer; 404 while it is being generated, when there is none, or when it was flagged. */
    public QuestionAiAnswerResponse questionAnswer(Long questionId, String lang) {
        return questionAiAnswerRepository.findByQuestionId(questionId)
                .filter(aiAnswer -> aiAnswer.getStatus() != AiAnswerStatus.FLAGGED)
                .map(aiAnswer -> QuestionAiAnswerResponse.from(aiAnswer, questionId))
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_AI_ANSWER_NOT_FOUND, lang));
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

    /** The caller's own questions and answers, newest first. */
    public Page<AiHistoryItemResponse> history(int pageNo, int pageSize, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        Pageable pageable = CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "creationDate"));
        return aiRepositories.findAllByUserId(userId, pageable).map(AiService::toHistoryItem);
    }

    private static AiHistoryItemResponse toHistoryItem(AiAnswer aiAnswer) {
        return new AiHistoryItemResponse(aiAnswer.getId(), aiAnswer.getQuestion(), aiAnswer.getAiAnswer(),
                aiAnswer.getCreationDate().atZone(ZoneId.systemDefault()).toInstant());
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
