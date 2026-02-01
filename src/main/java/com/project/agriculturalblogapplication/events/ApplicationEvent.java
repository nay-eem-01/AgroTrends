package com.project.agriculturalblogapplication.events;

import com.project.agriculturalblogapplication.model.AuditModel;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEvent extends AuditModel<String> {
    private String eventId;
    private String eventType;
    private OffsetDateTime timestamp;
    private Long actorId;
}
