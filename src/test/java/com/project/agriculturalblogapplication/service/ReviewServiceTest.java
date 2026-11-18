package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.Question;
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
import com.project.agriculturalblogapplication.model.response.AnswerResponse;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    private static final String LANG = "en";
    private static final long REVIEWER = 7L;
    private static final long TOPIC = 3L;
    private static final long ASKER = 2L;

    private final QuestionAiAnswerRepository repository = mock(QuestionAiAnswerRepository.class);
    private final AnswerService answerService = mock(AnswerService.class);
    private final ReputationService reputation = mock(ReputationService.class);
    private final ExpertService expertService = mock(ExpertService.class);
    private final UserService userService = mock(UserService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final NotificationPublisher notifications = mock(NotificationPublisher.class);
    private final ReviewService reviewService = new ReviewService(repository, answerService, reputation, expertService,
            userService, authorization, events, notifications);

    private final User reviewer = new User();
    private QuestionAiAnswer aiAnswer;

    @BeforeEach
    void setUp() {
        reviewer.setId(REVIEWER);
        Category topic = new Category();
        topic.setId(TOPIC);
        Question question = new Question();
        question.setId(4L);
        question.setCategory(topic);
        User asker = new User();
        asker.setId(ASKER);
        question.setUser(asker);
        aiAnswer = new QuestionAiAnswer();
        aiAnswer.setId(12L);
        aiAnswer.setQuestion(question);
        when(repository.findByQuestionId(4L)).thenReturn(Optional.of(aiAnswer));
        when(authorization.currentUserId(LANG)).thenReturn(REVIEWER);
        when(userService.findByIdWithException(REVIEWER, LANG)).thenReturn(reviewer);
        when(repository.transition(eq(12L), anyCollection(), any(), any(), any(), any())).thenReturn(1);
    }

    private void standing(Set<Long> topContributorIn, Set<Long> expertIn) {
        when(reputation.standings(List.of(REVIEWER)))
                .thenReturn(Map.of(REVIEWER, new AuthorStanding(0, topContributorIn, expertIn)));
        when(expertService.verifiedTopics(List.of(REVIEWER))).thenReturn(Map.of(REVIEWER, expertIn));
    }

    @Test
    void anExpertInTheTopicConfirms() {
        standing(Set.of(), Set.of(TOPIC));

        reviewService.confirm(4L, LANG);

        verify(repository).transition(eq(12L), eq(Set.of(AiAnswerStatus.UNCHECKED)), eq(AiAnswerStatus.CONFIRMED),
                eq(reviewer), any(), isNull());
        verify(events).publishEvent(new AiAnswerReviewed(4L));
        verify(notifications).publish(new NotificationMessage(ASKER, REVIEWER, NotificationType.AI_ANSWER_REVIEWED,
                NotificationTarget.QUESTION, 4L));
    }

    @Test
    void anExpertInAnotherTopicOrATopContributorCannotConfirm() {
        standing(Set.of(TOPIC), Set.of(99L));

        ApplicationException e = assertThrows(ApplicationException.class, () -> reviewService.confirm(4L, LANG));

        assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
        verify(repository, never()).transition(any(), anyCollection(), any(), any(), any(), any());
    }

    @Test
    void aQuestionWithoutATopicCannotBeReviewed() {
        standing(Set.of(), Set.of(TOPIC));
        aiAnswer.getQuestion().setCategory(null);

        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApplicationException.class, () -> reviewService.confirm(4L, LANG)).getHttpStatus());
    }

    @Test
    void losingTheRaceToAnotherReviewerIsAConflict() {
        standing(Set.of(), Set.of(TOPIC));
        when(repository.transition(eq(12L), anyCollection(), any(), any(), any(), any())).thenReturn(0);

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApplicationException.class, () -> reviewService.confirm(4L, LANG)).getHttpStatus());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void correctingPostsTheExpertsAnswerAndLinksIt() {
        standing(Set.of(), Set.of(TOPIC));
        AnswerResponse posted = new AnswerResponse();
        posted.setAnswerId(30L);
        when(answerService.create(any(CreateAnswerRequest.class), eq(LANG))).thenReturn(posted);
        Answer correction = new Answer();
        when(answerService.findByIdWithException(30L)).thenReturn(correction);

        reviewService.correct(4L, "Spray only after testing the soil.", LANG);

        ArgumentCaptor<CreateAnswerRequest> answer = ArgumentCaptor.forClass(CreateAnswerRequest.class);
        verify(answerService).create(answer.capture(), eq(LANG));
        assertEquals(4L, answer.getValue().getQuestionId());
        assertEquals("Spray only after testing the soil.", answer.getValue().getContent());
        verify(repository).transition(eq(12L), eq(Set.of(AiAnswerStatus.UNCHECKED)), eq(AiAnswerStatus.CORRECTED),
                eq(reviewer), any(), eq(correction));
    }

    @Test
    void correctingAnAlreadyReviewedAnswerPostsNothing() {
        standing(Set.of(), Set.of(TOPIC));
        aiAnswer.setStatus(AiAnswerStatus.CONFIRMED);

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApplicationException.class, () -> reviewService.correct(4L, "x", LANG)).getHttpStatus());
        verify(answerService, never()).create(any(), any());
    }

    @Test
    void aTopContributorInTheTopicMayFlagButAnOrdinaryUserMayNot() {
        standing(Set.of(TOPIC), Set.of());
        reviewService.flag(4L, LANG);
        verify(repository).transition(eq(12L), eq(Set.of(AiAnswerStatus.UNCHECKED)), eq(AiAnswerStatus.FLAGGED),
                eq(reviewer), any(), isNull());

        when(reputation.standings(List.of(REVIEWER))).thenReturn(Map.of());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApplicationException.class, () -> reviewService.flag(4L, LANG)).getHttpStatus());
    }

    @Test
    void reopeningClearsTheReviewerAndOnlyAppliesToReviewedAnswers() {
        reviewService.reopen(4L, LANG);

        verify(repository).transition(eq(12L),
                eq(Set.of(AiAnswerStatus.CONFIRMED, AiAnswerStatus.CORRECTED, AiAnswerStatus.FLAGGED)),
                eq(AiAnswerStatus.UNCHECKED), isNull(), isNull(), isNull());
        verify(notifications, never()).publish(any());
    }

    @Test
    void theQueueIsForExpertsOnlyAndCoversTheirTopics() {
        when(reputation.standings(List.of(REVIEWER))).thenReturn(Map.of());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApplicationException.class, () -> reviewService.queue(0, 20, LANG)).getHttpStatus());

        standing(Set.of(), Set.of(TOPIC, 5L));
        when(repository.findAllByStatusAndQuestionCategoryIdInAndQuestionHiddenFalse(eq(AiAnswerStatus.UNCHECKED), eq(Set.of(TOPIC, 5L)), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        assertEquals(0, reviewService.queue(0, 20, LANG).getTotalElements());
    }

    @Test
    void dismissedReportsRestoreTheStatusBeforeTheHideButLeaveAnExpertsFlag() {
        when(repository.unhide(4L)).thenReturn(1);
        reviewService.restore(4L);
        verify(events).publishEvent(new AiAnswerReviewed(4L));

        when(repository.unhide(5L)).thenReturn(0);
        reviewService.restore(5L);
        verify(events, never()).publishEvent(new AiAnswerReviewed(5L));
    }

    @Test
    void noAiAnswerIsNotFound() {
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApplicationException.class, () -> reviewService.confirm(5L, LANG)).getHttpStatus());
    }
}
