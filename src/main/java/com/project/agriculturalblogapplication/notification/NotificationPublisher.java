package com.project.agriculturalblogapplication.notification;

/**
 * The one way services send notifications. Call it inside the transaction that makes the change: the message is
 * delivered only if that transaction commits (or at once when there is none).
 * <p>
 * The transport is configuration, not code: {@code app.notifications.transport=spring} (the default) uses in-process
 * Spring events. A broker (Kafka, RabbitMQ) is one more implementation of this interface selected by another value;
 * its consumer hands each message to {@code NotificationService.store}, so no caller changes.
 */
public interface NotificationPublisher {

    void publish(NotificationMessage message);
}
