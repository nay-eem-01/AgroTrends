package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.ReportTable;
import com.project.agriculturalblogapplication.enums.ReportReason;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import com.project.agriculturalblogapplication.model.AuditModel;
import com.project.agriculturalblogapplication.model.EnumNameConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** One user's report of one piece of content or one user; a user reports a target once. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = ReportTable.NAME, uniqueConstraints = @UniqueConstraint(
        columnNames = {ReportTable.REPORTER_ID, ReportTable.TARGET_TYPE, ReportTable.TARGET_ID}),
        indexes = @Index(columnList = ReportTable.STATUS))
public class Report extends AuditModel<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ReportTable.REPORTER_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User reporter;

    @Convert(converter = TargetConverter.class)
    @Column(name = ReportTable.TARGET_TYPE, nullable = false, length = 40)
    private ReportTarget targetType;

    /** The target's id; for AI_ANSWER, its question's id. Not a foreign key: targets live in several tables. */
    @Column(name = ReportTable.TARGET_ID, nullable = false)
    private Long targetId;

    // Converted, not @Enumerated, like every enum here: Hibernate's enum CHECK is never widened by ddl-auto.
    @Convert(converter = ReasonConverter.class)
    @Column(name = ReportTable.REASON, nullable = false, length = 40)
    private ReportReason reason;

    @Column(name = ReportTable.DETAILS, length = 500)
    private String details;

    /** What the removed content said, kept as evidence once the post itself is gone. */
    @Column(name = ReportTable.SNAPSHOT, columnDefinition = "TEXT")
    private String snapshot;

    @Convert(converter = StatusConverter.class)
    @Column(name = ReportTable.STATUS, nullable = false, length = 40)
    private ReportStatus status = ReportStatus.OPEN;

    @Converter
    static class ReasonConverter extends EnumNameConverter<ReportReason> {
        ReasonConverter() {
            super(ReportReason.class);
        }
    }

    @Converter
    static class TargetConverter extends EnumNameConverter<ReportTarget> {
        TargetConverter() {
            super(ReportTarget.class);
        }
    }

    @Converter
    static class StatusConverter extends EnumNameConverter<ReportStatus> {
        StatusConverter() {
            super(ReportStatus.class);
        }
    }
}
