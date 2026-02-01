package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.NotificationTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = NotificationTable.TABLE_NAME)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends AuditModel<String> {
    @Column(nullable = false, name = NotificationTable.USER_ID)
    private Long userId;

    @Column(nullable = false, name = NotificationTable.TYPE)
    private String type;

    @Column(nullable = false, name = NotificationTable.MESSAGE)
    private String message;

    @Column(name = NotificationTable.RELATED_ENTITY_ID)
    private Long relatedEntityId;

    @Column(name = NotificationTable.RELATED_ENTITY_TYPE)
    private String relatedEntityType;

    @Column(nullable = false, name = NotificationTable.IS_READ)
    private boolean isRead = false;

    @Column(name = NotificationTable.READ_AT)
    private OffsetDateTime readAt;
}
