package com.project.agriculturalblogapplication.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String type;
    private String message;
    private Long relatedEntityId;
    private String relatedEntityType;
    private boolean isRead;
    private OffsetDateTime readAt;
}