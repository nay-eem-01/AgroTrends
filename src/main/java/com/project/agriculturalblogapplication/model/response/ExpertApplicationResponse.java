package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.ExpertApplication;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import com.project.agriculturalblogapplication.enums.CredentialType;
import com.project.agriculturalblogapplication.util.CommonUtils;

import java.time.Instant;
import java.util.List;

/** Seen only by the applicant and admins: it carries the proof. */
public record ExpertApplicationResponse(Long id, Long userId, String applicantName, List<CategoryResponse> topics,
                                        String fieldTitle, CredentialType credentialType, String institution,
                                        List<String> proofImageUrls, List<String> links, ApplicationStatus status,
                                        String reviewNote, Instant createdAt, Instant updatedAt) {

    public static ExpertApplicationResponse from(ExpertApplication application) {
        return new ExpertApplicationResponse(application.getId(), application.getUser().getId(),
                application.getUser().getName(), application.getTopics().stream().map(CategoryResponse::from).toList(),
                application.getFieldTitle(), application.getCredentialType(), application.getInstitution(),
                List.copyOf(application.getProofImageUrls()), List.copyOf(application.getLinks()),
                application.getStatus(), application.getReviewNote(),
                CommonUtils.toInstant(application.getCreationDate()),
                CommonUtils.toInstant(application.getLastModifiedDate()));
    }
}
