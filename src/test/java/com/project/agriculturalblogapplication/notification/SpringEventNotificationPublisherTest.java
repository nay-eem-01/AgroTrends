package com.project.agriculturalblogapplication.notification;

import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SpringEventNotificationPublisherTest {

    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final SpringEventNotificationPublisher publisher = new SpringEventNotificationPublisher(events, notificationService);
    private final NotificationMessage message =
            new NotificationMessage(1L, 2L, NotificationType.ANSWER_ACCEPTED, NotificationTarget.QUESTION, 3L);

    @Test
    void publishingOnlyRaisesAnEventAndDeliveryStoresIt() {
        publisher.publish(message);
        verify(events).publishEvent(message);
        verifyNoInteractions(notificationService);

        publisher.deliver(message);
        verify(notificationService).store(message);
    }

    @Test
    void aFailedStoreIsLoggedNotThrown() {
        // Regression: without a transaction delivery runs inline, so a failure here failed the action itself.
        doThrow(new RuntimeException("recipient deleted")).when(notificationService).store(message);

        assertDoesNotThrow(() -> publisher.deliver(message));
    }

    @Test
    void itIsTheDefaultTransportAndDeliversAfterCommitOrAtOnceOutsideATransaction() throws Exception {
        ConditionalOnProperty condition = SpringEventNotificationPublisher.class.getAnnotation(ConditionalOnProperty.class);
        assertEquals("spring", condition.havingValue());
        assertTrue(condition.matchIfMissing());
        TransactionalEventListener listener = SpringEventNotificationPublisher.class
                .getMethod("deliver", NotificationMessage.class).getAnnotation(TransactionalEventListener.class);
        assertTrue(listener.fallbackExecution());
    }
}
