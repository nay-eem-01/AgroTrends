package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Notification;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.NotificationResponse;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.repositories.NotificationRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Stores and serves in-app notifications; whatever transport delivers a message hands it to {@link #store}. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;

    private final AuthorizationService authorizationService;

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

    /** The caller's notifications, newest first; only unread ones when {@code unreadOnly}. */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> mine(boolean unreadOnly, int pageNo, int pageSize, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        Pageable pageable = CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "creationDate"));
        return (unreadOnly ? notificationRepository.findAllByRecipientIdAndReadAtIsNull(userId, pageable)
                : notificationRepository.findAllByRecipientId(userId, pageable)).map(NotificationResponse::from);
    }

    public long unreadCount(String lang) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(authorizationService.currentUserId(lang));
    }

    /** Marks one of the caller's notifications read; someone else's is 404, as if it did not exist. */
    @Transactional
    public NotificationResponse markRead(Long notificationId, String lang) {
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, authorizationService.currentUserId(lang))
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_NOTIFICATION_NOT_FOUND, lang));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return NotificationResponse.from(notification);
    }

    /** Returns how many were unread. */
    @Transactional
    public int markAllRead(String lang) {
        return notificationRepository.markAllRead(authorizationService.currentUserId(lang), LocalDateTime.now());
    }
}
