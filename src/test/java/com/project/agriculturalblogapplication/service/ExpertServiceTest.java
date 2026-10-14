package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.ExpertApplication;
import com.project.agriculturalblogapplication.entities.ExpertBadge;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import com.project.agriculturalblogapplication.events.AiAnswerReviewed;
import com.project.agriculturalblogapplication.enums.CredentialType;
import com.project.agriculturalblogapplication.events.ExpertBadgeChanged;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.ExpertApplicationRequest;
import com.project.agriculturalblogapplication.model.request.ExpertDecisionRequest;
import com.project.agriculturalblogapplication.model.response.ExpertApplicationResponse;
import com.project.agriculturalblogapplication.repositories.ExpertApplicationRepository;
import com.project.agriculturalblogapplication.repositories.ExpertBadgeRepository;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.notification.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpertServiceTest {

    private static final String LANG = "en";
    private static final long APPLICANT = 7L;
    private static final long ADMIN = 1L;

    private final ExpertApplicationRepository repository = mock(ExpertApplicationRepository.class);
    private final UserService userService = mock(UserService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final ImageService imageService = mock(ImageService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final NotificationPublisher notifications = mock(NotificationPublisher.class);
    private final QuestionAiAnswerRepository aiAnswers = mock(QuestionAiAnswerRepository.class);
    private final ExpertBadgeRepository badgeRepository = mock(ExpertBadgeRepository.class);
    private final ExpertService expertService = new ExpertService(repository, badgeRepository, aiAnswers,
            userService, categoryService, imageService, authorization, events, notifications);

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(APPLICANT);
        user.setName("Rahim");
        when(authorization.currentUserId(LANG)).thenReturn(APPLICANT);
        when(userService.findByIdWithException(APPLICANT, LANG)).thenReturn(user);
        when(categoryService.findByIdWithException(3L)).thenReturn(category(3L, "Poultry"));
        when(imageService.requireUploaded(any(), any())).thenAnswer(call -> call.getArgument(0));
        when(repository.save(any(ExpertApplication.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.decide(any(), any(), any())).thenReturn(1);
    }

    @Test
    void anApplicationWaitsForReviewWithTheCallerAsApplicant() {
        ExpertApplicationResponse response = expertService.apply(request(List.of("/uploads/a.jpg"), null), LANG);

        assertEquals(ApplicationStatus.PENDING, response.status());
        assertEquals(APPLICANT, response.userId());
        assertEquals("Poultry", response.topics().getFirst().categoryName());
        assertEquals("Veterinarian", response.fieldTitle());
        assertEquals(List.of(), response.links());
    }

    @Test
    void aLinkAloneIsEnoughProof() {
        ExpertApplicationResponse response = expertService.apply(request(null, List.of("https://bau.edu.bd/me")), LANG);

        assertEquals(List.of("https://bau.edu.bd/me"), response.links());
    }

    @Test
    void anApplicationWithoutProofIsRefused() {
        ApplicationException e = assertThrows(ApplicationException.class,
                () -> expertService.apply(request(List.of(), List.of()), LANG));

        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void aSecondApplicationWhileOneIsPendingIsAConflict() {
        when(repository.existsByUserIdAndStatus(APPLICANT, ApplicationStatus.PENDING)).thenReturn(true);

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> expertService.apply(request(List.of("/uploads/a.jpg"), null), LANG));

        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void proofImagesMustHaveBeenUploadedHere() {
        when(imageService.requireUploaded(any(), any()))
                .thenThrow(new ApplicationException(HttpStatus.BAD_REQUEST, "not uploaded", LANG));

        assertThrows(ApplicationException.class,
                () -> expertService.apply(request(List.of("https://evil.example/x.jpg"), null), LANG));
        verify(repository, never()).save(any());
    }

    @Test
    void approvingGrantsABadgeInEachRequestedTopic() {
        ExpertApplication application = pendingApplication(category(3L, "Poultry"), category(4L, "Fish"));

        ExpertApplicationResponse response = expertService.approve(9L, decision(null, "  Welcome "), LANG);

        assertEquals(ApplicationStatus.APPROVED, response.status());
        assertEquals("Welcome", response.reviewNote());
        ArgumentCaptor<ExpertBadge> badges = ArgumentCaptor.forClass(ExpertBadge.class);
        verify(badgeRepository, times(2)).save(badges.capture());
        assertEquals(Set.of(3L, 4L), badges.getAllValues().stream().map(b -> b.getTopic().getId()).collect(Collectors.toSet()));
        assertEquals("Veterinarian", badges.getValue().getFieldTitle());
        verify(events).publishEvent(new ExpertBadgeChanged(APPLICANT, 3L));
        verify(events).publishEvent(new ExpertBadgeChanged(APPLICANT, 4L));
        assertSame(application.getUser(), badges.getValue().getUser());
    }

    @Test
    void approvingASubsetGrantsOnlyThoseTopics() {
        pendingApplication(category(3L, "Poultry"), category(4L, "Fish"));

        expertService.approve(9L, decision(List.of(4L), null), LANG);

        ArgumentCaptor<ExpertBadge> badge = ArgumentCaptor.forClass(ExpertBadge.class);
        verify(badgeRepository).save(badge.capture());
        assertEquals(4L, badge.getValue().getTopic().getId());
    }

    @Test
    void approvingATopicThatWasNotRequestedIsRefused() {
        ExpertApplication application = pendingApplication(category(3L, "Poultry"));

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> expertService.approve(9L, decision(List.of(3L, 5L), null), LANG));

        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        assertEquals(ApplicationStatus.PENDING, application.getStatus());
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void approvingAgainRestoresARevokedBadgeInsteadOfAddingOne() {
        pendingApplication(category(3L, "Poultry"));
        ExpertBadge revoked = new ExpertBadge();
        revoked.setRevokedAt(LocalDateTime.now());
        when(badgeRepository.findByUserIdAndTopicId(APPLICANT, 3L)).thenReturn(Optional.of(revoked));

        expertService.approve(9L, decision(null, null), LANG);

        verify(badgeRepository).save(revoked);
        assertNull(revoked.getRevokedAt());
    }

    @Test
    void aDecidedApplicationCannotBeDecidedAgain() {
        pendingApplication(category(3L, "Poultry")).setStatus(ApplicationStatus.REJECTED);

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> expertService.approve(9L, decision(null, null), LANG));

        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void anotherAdminDecidingFirstIsAConflictAndGrantsNothing() {
        // Regression: two admins could both pass the PENDING check, one approving and one rejecting.
        pendingApplication(category(3L, "Poultry"));
        when(repository.decide(9L, ApplicationStatus.APPROVED, null)).thenReturn(0);

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApplicationException.class, () -> expertService.approve(9L, decision(null, null), LANG)).getHttpStatus());
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void revokingReopensTheAiAnswersTheyConfirmedInThatTopic() {
        when(badgeRepository.findByUserIdAndTopicId(APPLICANT, 3L)).thenReturn(Optional.of(new ExpertBadge()));
        when(aiAnswers.reviewedBy(APPLICANT, 3L, AiAnswerStatus.CONFIRMED)).thenReturn(List.of(40L, 41L));

        expertService.revoke(APPLICANT, 3L, LANG);

        verify(aiAnswers).reopen(List.of(40L, 41L));
        verify(events).publishEvent(new AiAnswerReviewed(40L));
        verify(events).publishEvent(new AiAnswerReviewed(41L));
    }

    @Test
    void rejectingNeedsAReason() {
        when(authorization.currentUserId(LANG)).thenReturn(ADMIN);
        ExpertApplication application = pendingApplication(category(3L, "Poultry"));

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> expertService.reject(9L, decision(null, "  "), LANG));
        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());

        assertEquals(ApplicationStatus.REJECTED, expertService.reject(9L, decision(null, "Blurry proof"), LANG).status());
        assertEquals("Blurry proof", application.getReviewNote());
        verify(notifications).publish(new NotificationMessage(APPLICANT, null, NotificationType.APPLICATION_DECIDED,
                NotificationTarget.EXPERT_APPLICATION, 9L));
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void revokingStampsTheBadgeAndAnAlreadyRevokedOneIsNotFound() {
        ExpertBadge badge = new ExpertBadge();
        when(badgeRepository.findByUserIdAndTopicId(APPLICANT, 3L)).thenReturn(Optional.of(badge));

        expertService.revoke(APPLICANT, 3L, LANG);

        assertNotNull(badge.getRevokedAt());
        verify(events).publishEvent(new ExpertBadgeChanged(APPLICANT, 3L));
        ApplicationException e = assertThrows(ApplicationException.class, () -> expertService.revoke(APPLICANT, 3L, LANG));
        assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus());
    }

    @Test
    void verifiedTopicsAreGroupedPerUser() {
        when(badgeRepository.activeTopics(List.of(1L, 2L))).thenReturn(List.of(
                new Object[]{1L, 3L}, new Object[]{1L, 4L}, new Object[]{2L, 3L}));

        assertEquals(Map.of(1L, Set.of(3L, 4L), 2L, Set.of(3L)), expertService.verifiedTopics(List.of(1L, 2L)));
        assertEquals(Map.of(), expertService.verifiedTopics(List.of()));
    }

    private ExpertApplication pendingApplication(Category... topics) {
        User user = new User();
        user.setId(APPLICANT);
        ExpertApplication application = new ExpertApplication();
        application.setId(9L);
        application.setUser(user);
        application.setTopics(new LinkedHashSet<>(List.of(topics)));
        application.setFieldTitle("Veterinarian");
        when(repository.findById(9L)).thenReturn(Optional.of(application));
        return application;
    }

    private static ExpertDecisionRequest decision(List<Long> categoryIds, String note) {
        ExpertDecisionRequest decision = new ExpertDecisionRequest();
        decision.setCategoryIds(categoryIds);
        decision.setNote(note);
        return decision;
    }

    private static ExpertApplicationRequest request(List<String> images, List<String> links) {
        ExpertApplicationRequest request = new ExpertApplicationRequest();
        request.setCategoryIds(List.of(3L, 3L));
        request.setFieldTitle("  Veterinarian ");
        request.setCredentialType(CredentialType.DEGREE);
        request.setInstitution("Bangladesh Agricultural University");
        request.setProofImageUrls(images);
        request.setLinks(links);
        return request;
    }

    private static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setCategoryName(name);
        return category;
    }
}
