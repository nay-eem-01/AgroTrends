package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Notification;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.repositories.NotificationRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Stores and serves in-app notifications; whatever transport delivers a message hands it to {@link #store}. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;

    /**
     * Saves one notification, skipping self-notifications. Its own transaction: transports call it after the
     * publishing transaction has committed, when joining that transaction would never commit the row.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void store(NotificationMessage message) {
        if (message.recipientId() == null || message.recipientId().equals(message.actorId())) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipient(userRepository.getReferenceById(message.recipientId()));
        notification.setActor(message.actorId() == null ? null : userRepository.getReferenceById(message.actorId()));
        notification.setType(message.type());
        notification.setTargetType(message.targetType());
        notification.setTargetId(message.targetId());
        notificationRepository.save(notification);
    }
}
