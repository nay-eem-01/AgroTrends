package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Notification;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.repositories.NotificationRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final NotificationService notificationService = new NotificationService(repository, userRepository, authorization);

    @Test
    void aNotificationIsStoredUnreadForItsRecipient() {
        User recipient = new User();
        User actor = new User();
        when(userRepository.getReferenceById(1L)).thenReturn(recipient);
        when(userRepository.getReferenceById(2L)).thenReturn(actor);

        notificationService.store(new NotificationMessage(1L, 2L, NotificationType.ANSWER_ON_YOUR_QUESTION,
                NotificationTarget.QUESTION, 9L));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertSame(recipient, saved.getValue().getRecipient());
        assertSame(actor, saved.getValue().getActor());
        assertEquals(NotificationType.ANSWER_ON_YOUR_QUESTION, saved.getValue().getType());
        assertEquals(9L, saved.getValue().getTargetId());
        assertNull(saved.getValue().getReadAt());
    }

    @Test
    void markingReadOnlyFindsTheCallersOwnAndKeepsTheFirstReadTime() {
        when(authorization.currentUserId("en")).thenReturn(1L);
        Notification mine = new Notification();
        LocalDateTime firstRead = LocalDateTime.of(2026, 10, 1, 9, 0);
        mine.setReadAt(firstRead);
        when(repository.findByIdAndRecipientId(5L, 1L)).thenReturn(Optional.of(mine));

        assertTrue(notificationService.markRead(5L, "en").read());
        assertEquals(firstRead, mine.getReadAt());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApplicationException.class, () -> notificationService.markRead(6L, "en")).getHttpStatus());
    }

    @Test
    void anUnreadNotificationIsStampedWhenRead() {
        when(authorization.currentUserId("en")).thenReturn(1L);
        Notification unread = new Notification();
        when(repository.findByIdAndRecipientId(5L, 1L)).thenReturn(Optional.of(unread));

        notificationService.markRead(5L, "en");

        assertNotNull(unread.getReadAt());
    }

    @Test
    void theListIsTheCallersNewestFirstAndCanBeLimitedToUnread() {
        when(authorization.currentUserId("en")).thenReturn(1L);
        when(repository.findAllByRecipientIdAndReadAtIsNull(eq(1L), any())).thenReturn(Page.empty());

        notificationService.mine(true, 0, 500, "en");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByRecipientIdAndReadAtIsNull(eq(1L), pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "creationDate"), pageable.getValue().getSort());
        verify(repository, never()).findAllByRecipientId(any(), any());
    }

    @Test
    void nobodyIsNotifiedOfTheirOwnAction() {
        notificationService.store(new NotificationMessage(1L, 1L, NotificationType.ANSWER_ON_YOUR_QUESTION,
                NotificationTarget.QUESTION, 9L));

        verify(repository, never()).save(any());
    }

    @Test
    void aSystemNotificationHasNoActor() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());

        notificationService.store(new NotificationMessage(1L, null, NotificationType.NEW_REVIEW_ITEM,
                NotificationTarget.QUESTION, 9L));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertNull(saved.getValue().getActor());
    }
}
