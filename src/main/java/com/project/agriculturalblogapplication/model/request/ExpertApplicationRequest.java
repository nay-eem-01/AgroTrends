package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.enums.CredentialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** An application to be a Verified Expert; the applicant comes from the token. */
@Getter
@Setter
public class ExpertApplicationRequest {

    @NotEmpty(message = ErrorCode.ERROR_EXPERT_TOPICS_REQUIRED)
    @Size(max = AppConstants.MAX_EXPERT_TOPICS, message = ErrorCode.ERROR_EXPERT_TOPICS_REQUIRED)
    private List<@NotNull Long> categoryIds;

    /** e.g. "Veterinarian", "Agronomist". */
    @NotBlank(message = ErrorCode.ERROR_EXPERT_FIELD_TITLE_REQUIRED)
    @Size(max = 150, message = ErrorCode.ERROR_EXPERT_TEXT_TOO_LONG)
    private String fieldTitle;

    @NotNull(message = ErrorCode.ERROR_EXPERT_CREDENTIAL_TYPE_REQUIRED)
    private CredentialType credentialType;

    @NotBlank(message = ErrorCode.ERROR_EXPERT_INSTITUTION_REQUIRED)
    @Size(max = 150, message = ErrorCode.ERROR_EXPERT_TEXT_TOO_LONG)
    private String institution;

    /** Photos of the degree, staff ID or licence, uploaded through POST /api/images. */
    @Size(max = AppConstants.MAX_EXPERT_PROOF_IMAGES, message = ErrorCode.ERROR_EXPERT_TOO_MANY_PROOFS)
    private List<@NotBlank String> proofImageUrls;

    /** Pages that confirm the credential, e.g. a faculty profile. */
    @Size(max = AppConstants.MAX_EXPERT_LINKS, message = ErrorCode.ERROR_EXPERT_TOO_MANY_PROOFS)
    private List<@NotNull @Pattern(regexp = "https?://\\S{1,240}", message = ErrorCode.ERROR_EXPERT_INVALID_LINK) String> links;
}
