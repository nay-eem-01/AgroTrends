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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private final NotificationService notificationService = new NotificationService(repository, userRepository);

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
