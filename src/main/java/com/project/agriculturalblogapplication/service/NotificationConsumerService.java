package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Comment;
import com.project.agriculturalblogapplication.entities.Notification;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.events.CommentCreatedEvent;
import com.project.agriculturalblogapplication.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumerService {

    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "comment-events", groupId = "blog-app-group-v2")
    public void consumeCommentEvent(CommentCreatedEvent event) {
        log.info("Received comment created event: {}", event);

        if (event.getCommenterUserId().equals(event.getBlogAuthorId())) {
            log.info("Author commented on their own blog, skipping notification");
            return;
        }

        Notification notification = Notification.builder()
                .userId(event.getBlogAuthorId())
                .type(NotificationType.COMMENT_CREATED.name())
                .message(event.getCommenterUserName() + "commented on your blog")
                .relatedEntityId(event.getCommentId())
                .relatedEntityType(Comment.class.getSimpleName())
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Notification created for user {}", event.getBlogAuthorId());
    }
}
