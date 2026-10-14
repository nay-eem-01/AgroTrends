package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Notification;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.util.CommonUtils;

import java.time.Instant;

/** One notification: what happened ({@code type}), who did it, and what to open ({@code targetType}, {@code targetId}). */
public record NotificationResponse(Long id, NotificationType type, NotificationTarget targetType, Long targetId,
                                   Long actorId, String actorName, boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTargetType(),
                notification.getTargetId(),
                notification.getActor() == null ? null : notification.getActor().getId(),
                notification.getActor() == null ? null : notification.getActor().getName(),
                notification.getReadAt() != null, CommonUtils.toInstant(notification.getCreationDate()));
    }
}
