package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.events.AiAnswerReviewed;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.notification.NotificationPublisher;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.AuthorStanding;
import com.project.agriculturalblogapplication.model.request.CreateAnswerRequest;
import com.project.agriculturalblogapplication.model.response.QuestionAiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.ReviewItemResponse;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Expert review of AI answers on questions (roadmap 7.4). Verified Experts in a question's topic confirm or correct
 * its AI answer; they and Top Contributors in the topic may flag it; an admin may reopen any decision.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final QuestionAiAnswerRepository aiAnswerRepository;

    private final AnswerService answerService;

    private final ReputationService reputationService;

    private final ExpertService expertService;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    private final ApplicationEventPublisher eventPublisher;

    private final NotificationPublisher notificationPublisher;

    /** Unchecked AI answers in the caller's expert topics, oldest first. */
    @Transactional(readOnly = true)
    public Page<ReviewItemResponse> queue(int pageNo, int pageSize, String lang) {
        Set<Long> topics = expertTopicsOf(authorizationService.currentUserId(lang));
        if (topics.isEmpty()) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_NOT_AN_EXPERT, lang);
        }
        return aiAnswerRepository.findAllByStatusAndQuestionCategoryIdIn(AiAnswerStatus.UNCHECKED, topics,
                        CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "creationDate")))
                .map(ReviewItemResponse::from);
    }

    @Transactional
    public QuestionAiAnswerResponse confirm(Long questionId, String lang) {
        QuestionAiAnswer aiAnswer = find(questionId, lang);
        User reviewer = expertIn(aiAnswer, lang);
        return move(aiAnswer, Set.of(AiAnswerStatus.UNCHECKED), AiAnswerStatus.CONFIRMED, reviewer, null,
                ErrorCode.ERROR_AI_ANSWER_ALREADY_REVIEWED, lang);
    }

    /** Posts the expert's own answer and marks the AI answer CORRECTED; a lost race rolls the answer back too. */
    @Transactional
    public QuestionAiAnswerResponse correct(Long questionId, String content, String lang) {
        QuestionAiAnswer aiAnswer = find(questionId, lang);
        User reviewer = expertIn(aiAnswer, lang);
        if (aiAnswer.getStatus() != AiAnswerStatus.UNCHECKED) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_AI_ANSWER_ALREADY_REVIEWED, lang);
        }
        CreateAnswerRequest answer = new CreateAnswerRequest();
        answer.setQuestionId(questionId);
        answer.setContent(content);
        Answer correction = answerService.findByIdWithException(answerService.create(answer, lang).getAnswerId());
        return move(aiAnswer, Set.of(AiAnswerStatus.UNCHECKED), AiAnswerStatus.CORRECTED, reviewer, correction,
                ErrorCode.ERROR_AI_ANSWER_ALREADY_REVIEWED, lang);
    }

    /** Hides a wrong or unsafe AI answer. */
    @Transactional
    public QuestionAiAnswerResponse flag(Long questionId, String lang) {
        QuestionAiAnswer aiAnswer = find(questionId, lang);
        Long userId = authorizationService.currentUserId(lang);
        Long topicId = topicOf(aiAnswer);
        AuthorStanding standing = standing(userId);
        if (topicId == null || !(standing.expertTopics().contains(topicId) || standing.topContributorTopics().contains(topicId))) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_CANNOT_FLAG_AI_ANSWER, lang);
        }
        return move(aiAnswer, Set.of(AiAnswerStatus.UNCHECKED), AiAnswerStatus.FLAGGED,
                userService.findByIdWithException(userId, lang), null, ErrorCode.ERROR_AI_ANSWER_ALREADY_REVIEWED, lang);
    }

    /**
     * Moderator: hides an AI answer whatever its review state and returns what it said. No reviewer is recorded, so
     * the asker is not notified and the moderator stays anonymous.
     */
    @Transactional
    public String hide(Long questionId, String lang) {
        QuestionAiAnswer aiAnswer = find(questionId, lang);
        move(aiAnswer, Set.of(AiAnswerStatus.UNCHECKED, AiAnswerStatus.CONFIRMED, AiAnswerStatus.CORRECTED),
                AiAnswerStatus.FLAGGED, null, null, ErrorCode.ERROR_AI_ANSWER_ALREADY_REVIEWED, lang);
        return aiAnswer.getAnswer();
    }

    /** Admin: back to UNCHECKED. A correction stays posted as an ordinary answer. */
    @Transactional
    public QuestionAiAnswerResponse reopen(Long questionId, String lang) {
        return move(find(questionId, lang), Set.of(AiAnswerStatus.CONFIRMED, AiAnswerStatus.CORRECTED, AiAnswerStatus.FLAGGED),
                AiAnswerStatus.UNCHECKED, null, null, ErrorCode.ERROR_AI_ANSWER_NOT_REVIEWED, lang);
    }

    private QuestionAiAnswer find(Long questionId, String lang) {
        return aiAnswerRepository.findByQuestionId(questionId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_AI_ANSWER_NOT_FOUND, lang));
    }

    /** The caller, if they are a Verified Expert in the question's topic; 403 otherwise. */
    private User expertIn(QuestionAiAnswer aiAnswer, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        Long topicId = topicOf(aiAnswer);
        if (topicId == null || !expertTopicsOf(userId).contains(topicId)) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_NOT_EXPERT_IN_TOPIC, lang);
        }
        return userService.findByIdWithException(userId, lang);
    }

    private static Long topicOf(QuestionAiAnswer aiAnswer) {
        return aiAnswer.getQuestion().getCategory() == null ? null : aiAnswer.getQuestion().getCategory().getId();
    }

    private Set<Long> expertTopicsOf(Long userId) {
        return expertService.verifiedTopics(List.of(userId)).getOrDefault(userId, Set.of());
    }

    /** Badges and points; only flag needs it, since Top Contributors may flag too. */
    private AuthorStanding standing(Long userId) {
        return reputationService.standings(List.of(userId)).getOrDefault(userId, AuthorStanding.NONE);
    }

    private QuestionAiAnswerResponse move(QuestionAiAnswer aiAnswer, Set<AiAnswerStatus> from, AiAnswerStatus to,
                                          User reviewer, Answer correction, String conflict, String lang) {
        int moved = aiAnswerRepository.transition(aiAnswer.getId(), from, to, reviewer,
                reviewer == null ? null : LocalDateTime.now(), correction);
        if (moved == 0) {
            throw new ApplicationException(HttpStatus.CONFLICT, conflict, lang);
        }
        eventPublisher.publishEvent(new AiAnswerReviewed(aiAnswer.getQuestion().getId()));
        if (reviewer != null) {
            notificationPublisher.publish(new NotificationMessage(aiAnswer.getQuestion().getUser().getId(), reviewer.getId(),
                    NotificationType.AI_ANSWER_REVIEWED, NotificationTarget.QUESTION, aiAnswer.getQuestion().getId()));
        }
        return QuestionAiAnswerResponse.from(find(aiAnswer.getQuestion().getId(), lang));
    }
}
