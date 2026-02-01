package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Notification;
import com.project.agriculturalblogapplication.model.response.NotificationResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.NotificationRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Page<NotificationResponse> getAllByUserId(PaginationArgs paginationArgs, Long userId){
        Pageable pageable = CommonUtils.getPageable(paginationArgs);

        List<Notification> notifications = notificationRepository.findAllByUserId(userId);

        List<NotificationResponse> notificationResponses = notifications.stream()
                .map(this::mapToResponse)
                .toList();

        return new PageImpl<>(notificationResponses, pageable, notificationResponses.size());
    }

    public Page<NotificationResponse> getAllUnreadNotifications(PaginationArgs paginationArgs, Long userId) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);

        List<Notification> notifications =  notificationRepository.findAllByUserIdAndIsReadFalse(userId);

        List<NotificationResponse> notificationResponses = notifications.stream()
                .map(this::mapToResponse)
                .toList();

        return new PageImpl<>(notificationResponses, pageable, notificationResponses.size());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        notification.setReadAt(OffsetDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications =
                notificationRepository.findAllByUserIdAndIsReadFalse(userId);

        notifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(OffsetDateTime.now());
        });

        notificationRepository.saveAll(notifications);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .build();
    }
}
