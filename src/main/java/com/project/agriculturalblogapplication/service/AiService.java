package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AiProperties;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.AgriMetadata;
import com.project.agriculturalblogapplication.entities.AiAnswer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.events.QuestionCreated;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.notification.NotificationPublisher;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.AskExpertsRequest;
import com.project.agriculturalblogapplication.model.request.CreateQuestionRequest;
import com.project.agriculturalblogapplication.model.response.AiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.AiHistoryItemResponse;
import com.project.agriculturalblogapplication.model.response.AiSourceResponse;
import com.project.agriculturalblogapplication.model.response.QuestionAiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.QuestionResponse;
import com.project.agriculturalblogapplication.repositories.AiRepositories;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    static final int TITLE_LENGTH = 120;

    static final String QUESTION_ANSWERS_KEY = "ai-question-answers:";

    private final AiRepositories aiRepositories;

    private final ChatClient chatClient;

    private final AuthorizationService authorizationService;

    private final AiProperties aiProperties;

    private final QuestionService questionService;

    private final QuestionAiAnswerRepository questionAiAnswerRepository;

    private final AttemptLimiter attemptLimiter;

    private final ExpertService expertService;

    private final NotificationPublisher notificationPublisher;

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
        aiAnswer.setSources(sourcesOf(response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT)));
        aiAnswer.setPromptTokens(usage.getPromptTokens());
        aiAnswer.setCompletionTokens(usage.getCompletionTokens());
        aiRepositories.save(aiAnswer);

        return new AiAnswerResponse(answer, aiAnswer.getSources());
    }

    /**
     * "Ask an expert to check this": the caller's private ask becomes a public question in a topic, and its answer
     * becomes that question's unchecked AI answer, so it lands in the experts' review queue without a new model call.
     */
    @Transactional
    public QuestionResponse askExperts(Long aiAnswerId, AskExpertsRequest request, String lang) {
        AiAnswer ask = aiRepositories.findByIdAndUserId(aiAnswerId, authorizationService.currentUserId(lang)).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_AI_ANSWER_NOT_FOUND, lang));
        if (ask.getPublishedQuestion() != null) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_AI_ASK_ALREADY_PUBLISHED, lang);
        }
        CreateQuestionRequest create = new CreateQuestionRequest();
        create.setTitle(request.getTitle() == null || request.getTitle().isBlank() ? titleOf(ask.getQuestion()) : request.getTitle().strip());
        create.setContent(ask.getQuestion());
        create.setCategoryId(request.getCategoryId());
        QuestionResponse created = questionService.create(create, lang);
        Question question = questionService.findByIdWithException(created.getQuestionId());
        if (aiRepositories.markPublished(ask.getId(), question) == 0) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_AI_ASK_ALREADY_PUBLISHED, lang);
        }

        QuestionAiAnswer aiAnswer = new QuestionAiAnswer();
        aiAnswer.setQuestion(question);
        aiAnswer.setAnswer(ask.getAiAnswer());
        aiAnswer.setSources(ask.getSources());
        questionAiAnswerRepository.save(aiAnswer);
        notifyReviewers(question);
        return created;
    }

    /** Tells every Verified Expert in the topic that the AI answer waits; the asker is the actor, so never themselves. */
    // shortcut: one row per expert per question; switch to a digest if topics grow past a few dozen experts.
    private void notifyReviewers(Question question) {
        notifyExperts(question, NotificationType.NEW_REVIEW_ITEM);
    }

    private void notifyExperts(Question question, NotificationType type) {
        if (question.getCategory() == null) {
            return;
        }
        for (Long expertId : expertService.expertsIn(question.getCategory().getId())) {
            notificationPublisher.publish(new NotificationMessage(expertId, question.getUser().getId(), type,
                    NotificationTarget.QUESTION, question.getId()));
        }
    }

    /** The first line of the asked text, cut to fit a title. */
    static String titleOf(String asked) {
        String firstLine = asked.strip().lines().findFirst().orElse("");
        return firstLine.length() <= TITLE_LENGTH ? firstLine : firstLine.substring(0, TITLE_LENGTH - 1).strip() + "…";
    }

    /**
     * Answers a new community question once it is committed, off the request thread. Never throws: a failure or
     * the asker's daily limit just leaves the question without an AI answer, and the topic's experts still hear of it.
     */
    // shortcut: a failed generation is not retried; add a retry sweep if Gemini outages leave many questions bare.
    @Async
    @TransactionalEventListener
    public void answerQuestion(QuestionCreated event) {
        Question question = null;
        try {
            if (questionAiAnswerRepository.existsByQuestionId(event.questionId())) {
                return; // sent from a private ask (askExperts): it already has its answer
            }
            question = questionService.findByIdWithException(event.questionId());
            // Counted in memory, not from stored answers: deleting questions must not give the allowance back, and
            // recording before the slow model call stops parallel questions from all slipping under the limit.
            String allowance = QUESTION_ANSWERS_KEY + question.getUser().getId();
            if (attemptLimiter.isBlocked(allowance)) {
                log.info("No AI answer for question {}: the asker reached today's limit", question.getId());
                notifyExperts(question, NotificationType.NEW_QUESTION_IN_TOPIC);
                return;
            }
            attemptLimiter.record(allowance, aiProperties.getDailyQuestionLimit(), Duration.ofDays(1));
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
            notifyReviewers(question);
        } catch (RuntimeException e) {
            log.error("AI answer for question {} failed", event.questionId(), e);
            if (question != null) {
                notifyExperts(question, NotificationType.NEW_QUESTION_IN_TOPIC);
            }
        }
    }

    /**
     * Title and plain text, capped like a private question so one long post cannot run up the bill, then the
     * question's crop, season, region and soil so the advice fits the asker's farm (each at most 60 characters).
     */
    static String promptFor(Question question) {
        String text = question.getTitle() + "\n\n" + SafeHtml.plainText(question.getContent());
        if (text.length() > AppConstants.AI_MAX_QUESTION_LENGTH) {
            text = text.substring(0, AppConstants.AI_MAX_QUESTION_LENGTH);
        }
        String details = farmDetails(question.getAgri());
        return details.isEmpty() ? text : text + "\n\nFarm details: " + details;
    }

    private static String farmDetails(AgriMetadata agri) {
        if (agri == null) {
            return "";
        }
        List<String> details = new ArrayList<>();
        if (agri.getCrop() != null && !agri.getCrop().isBlank()) {
            details.add("crop " + agri.getCrop().strip());
        }
        if (agri.getSeason() != null) {
            details.add("season " + agri.getSeason().name().toLowerCase().replace('_', '-'));
        }
        if (agri.getRegion() != null && !agri.getRegion().isBlank()) {
            details.add("region " + agri.getRegion().strip());
        }
        if (agri.getSoil() != null) {
            details.add("soil " + agri.getSoil().name().toLowerCase().replace('_', ' '));
        }
        return String.join("; ", details);
    }

    /** The question's AI answer; 404 while it is being generated, when there is none, or when it was flagged. */
    public QuestionAiAnswerResponse questionAnswer(Long questionId, String lang) {
        return questionAiAnswerRepository.findByQuestionId(questionId)
                .filter(aiAnswer -> aiAnswer.getStatus() != AiAnswerStatus.FLAGGED && !aiAnswer.getQuestion().isHidden())
                .map(QuestionAiAnswerResponse::from)
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
                List.copyOf(aiAnswer.getSources()),
                aiAnswer.getPublishedQuestion() == null ? null : aiAnswer.getPublishedQuestion().getId(),
                aiAnswer.getCreationDate().atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * One source per post or question, in retrieval order: several chunks of the same post, or several vetted
     * answers to the same question, collapse into one citation.
     */
    static List<AiSourceResponse> sourcesOf(Object retrieved) {
        if (!(retrieved instanceof List<?> documents)) {
            return List.of();
        }
        Map<String, AiSourceResponse> sources = new LinkedHashMap<>();
        for (Object item : documents) {
            if (!(item instanceof Document document)) {
                continue;
            }
            Map<String, Object> metadata = document.getMetadata();
            String title = (String) metadata.get(DocumentService.TITLE);
            if (metadata.get(DocumentService.BLOG_ID) instanceof Number blogId) {
                sources.putIfAbsent("blog:" + blogId, AiSourceResponse.blog(blogId.longValue(), title));
            } else if (DocumentService.ANSWER_TYPE.equals(metadata.get(DocumentService.TYPE))
                    && metadata.get(DocumentService.QUESTION_ID) instanceof Number questionId) {
                sources.putIfAbsent("question:" + questionId, AiSourceResponse.answer(questionId.longValue(), title));
            }
        }
        return List.copyOf(sources.values());
    }
}
