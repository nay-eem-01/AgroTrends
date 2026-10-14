package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.enums.ReportReason;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** A report of content or a user; the reporter comes from the token. */
@Getter
@Setter
public class CreateReportRequest {

    @NotNull(message = ErrorCode.ERROR_REPORT_TARGET_REQUIRED)
    private ReportTarget targetType;

    /** The target's id; for AI_ANSWER, the question's id. */
    @NotNull(message = ErrorCode.ERROR_REPORT_TARGET_REQUIRED)
    private Long targetId;

    @NotNull(message = ErrorCode.ERROR_REPORT_REASON_REQUIRED)
    private ReportReason reason;

    @Size(max = 500, message = ErrorCode.ERROR_REPORT_DETAILS_TOO_LONG)
    private String details;
}
