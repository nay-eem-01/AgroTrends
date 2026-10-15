package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Report;
import com.project.agriculturalblogapplication.enums.ReportReason;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import com.project.agriculturalblogapplication.util.CommonUtils;

import java.time.Instant;

/** {@code snapshot}: what removed content said (set when a moderator removes it). */
public record ReportResponse(Long id, Long reporterId, String reporterName, ReportTarget targetType, Long targetId,
                             ReportReason reason, String details, ReportStatus status, String snapshot,
                             Instant createdAt) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(report.getId(), report.getReporter().getId(), report.getReporter().getName(),
                report.getTargetType(), report.getTargetId(), report.getReason(), report.getDetails(),
                report.getStatus(), report.getSnapshot(), CommonUtils.toInstant(report.getCreationDate()));
    }
}
