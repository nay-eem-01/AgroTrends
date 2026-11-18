package com.project.agriculturalblogapplication.notification;

import com.project.agriculturalblogapplication.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** In-process transport: a Spring event, stored after the publishing transaction commits. */
// shortcut: lost if the app stops between commit and store, and runs in one instance only; switch the transport to a
// broker (or add an outbox) when a second instance or consumer appears.
@Slf4j
@Component
@ConditionalOnProperty(name = "app.notifications.transport", havingValue = "spring", matchIfMissing = true)
@RequiredArgsConstructor
public class SpringEventNotificationPublisher implements NotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;

    private final NotificationService notificationService;

    @Override
    public void publish(NotificationMessage message) {
        eventPublisher.publishEvent(message);
    }

    // fallbackExecution: background work (the AI answer on a question) publishes outside any transaction.
    // Caught: without a transaction this runs inline, and a lost notification must not fail the action itself.
    @TransactionalEventListener(fallbackExecution = true)
    public void deliver(NotificationMessage message) {
        try {
            notificationService.store(message);
        } catch (RuntimeException e) {
            log.warn("Could not store notification {} for user {}", message.type(), message.recipientId(), e);
        }
    }
}
