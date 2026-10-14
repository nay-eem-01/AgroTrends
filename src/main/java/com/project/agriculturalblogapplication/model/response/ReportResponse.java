package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Report;
import com.project.agriculturalblogapplication.enums.ReportReason;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import com.project.agriculturalblogapplication.util.CommonUtils;

import java.time.Instant;

public record ReportResponse(Long id, ReportTarget targetType, Long targetId, ReportReason reason, String details,
                             ReportStatus status, Instant createdAt) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(report.getId(), report.getTargetType(), report.getTargetId(), report.getReason(),
                report.getDetails(), report.getStatus(), CommonUtils.toInstant(report.getCreationDate()));
    }
}
