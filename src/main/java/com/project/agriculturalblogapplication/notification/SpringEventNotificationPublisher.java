package com.project.agriculturalblogapplication.notification;

import com.project.agriculturalblogapplication.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** In-process transport: a Spring event, stored after the publishing transaction commits. */
// shortcut: lost if the app stops between commit and store, and runs in one instance only; switch the transport to a
// broker (or add an outbox) when a second instance or consumer appears.
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
    @TransactionalEventListener(fallbackExecution = true)
    public void deliver(NotificationMessage message) {
        notificationService.store(message);
    }
}
