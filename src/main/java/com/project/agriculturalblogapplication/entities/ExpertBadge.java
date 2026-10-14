package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.CategoryTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.ExpertApplicationTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.ExpertBadgeTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * A Verified Expert in one topic. One row per user and topic: revoking stamps {@code revokedAt}, granting again
 * clears it. {@code createdBy} / {@code lastModifiedBy} record which admin granted or revoked it.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = ExpertBadgeTable.NAME,
        uniqueConstraints = @UniqueConstraint(columnNames = {UserTable.USER_ID, CategoryTable.CATEGORY_ID}))
public class ExpertBadge extends AuditModel<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UserTable.USER_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = CategoryTable.CATEGORY_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Category topic;

    /** From the approved application, e.g. "Veterinarian". */
    @Column(name = ExpertApplicationTable.FIELD_TITLE, nullable = false, length = 150)
    private String fieldTitle;

    @Column(name = ExpertBadgeTable.REVOKED_AT)
    private LocalDateTime revokedAt;
}
