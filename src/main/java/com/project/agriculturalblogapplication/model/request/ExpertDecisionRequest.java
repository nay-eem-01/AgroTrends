package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** An admin's decision on an expert application. */
@Getter
@Setter
public class ExpertDecisionRequest {

    /** Approve only: the requested topics to grant; omitted grants all of them. */
    private List<@NotNull Long> categoryIds;

    /** Shown to the applicant; required when rejecting. */
    @Size(max = 500, message = ErrorCode.ERROR_EXPERT_NOTE_TOO_LONG)
    private String note;
}
