package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.NotificationTable;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** One in-app notification for one user; unread while {@code readAt} is null. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = NotificationTable.NAME,
        indexes = @Index(columnList = NotificationTable.RECIPIENT_ID + ", " + NotificationTable.READ_AT))
public class Notification extends AuditModel<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = NotificationTable.RECIPIENT_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User recipient;

    /** Who caused it; null for the system or once that user is deleted. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = NotificationTable.ACTOR_ID)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User actor;

    // Converted, not @Enumerated: Hibernate puts a CHECK on enum columns that ddl-auto never widens, so a new type
    // would fail on existing databases.
    @Convert(converter = TypeConverter.class)
    @Column(name = NotificationTable.TYPE, nullable = false, length = 40)
    private NotificationType type;

    @Convert(converter = TargetConverter.class)
    @Column(name = NotificationTable.TARGET_TYPE, nullable = false, length = 40)
    private NotificationTarget targetType;

    @Column(name = NotificationTable.TARGET_ID, nullable = false)
    private Long targetId;

    @Column(name = NotificationTable.READ_AT)
    private LocalDateTime readAt;

    @Converter
    static class TypeConverter implements AttributeConverter<NotificationType, String> {
        @Override
        public String convertToDatabaseColumn(NotificationType type) {
            return type == null ? null : type.name();
        }

        @Override
        public NotificationType convertToEntityAttribute(String name) {
            return name == null ? null : NotificationType.valueOf(name);
        }
    }

    @Converter
    static class TargetConverter implements AttributeConverter<NotificationTarget, String> {
        @Override
        public String convertToDatabaseColumn(NotificationTarget target) {
            return target == null ? null : target.name();
        }

        @Override
        public NotificationTarget convertToEntityAttribute(String name) {
            return name == null ? null : NotificationTarget.valueOf(name);
        }
    }
}
