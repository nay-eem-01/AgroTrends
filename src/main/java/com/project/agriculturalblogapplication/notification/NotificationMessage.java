package com.project.agriculturalblogapplication.notification;

import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;

/**
 * One notification on its way to {@code recipientId}. Plain ids and enums only, so any transport can serialize it.
 * {@code actorId} is who caused it (null for the system); a user is never notified of their own action.
 */
public record NotificationMessage(Long recipientId, Long actorId, NotificationType type, NotificationTarget targetType,
                                  Long targetId) {
}
