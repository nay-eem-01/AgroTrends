package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.util.CommonUtils;

import java.time.Instant;
import java.util.List;

/**
 * The AI answer on a question, with its review status and the safety note the UI must show next to it.
 * {@code correctionAnswerId} points at the expert's answer when the status is CORRECTED.
 */
public record QuestionAiAnswerResponse(Long id, Long questionId, String answer, List<AiSourceResponse> sources,
                                       AiAnswerStatus status, String disclaimer, String reviewerName,
                                       Instant reviewedAt, Long correctionAnswerId, Instant createdAt) {

    public static final String DISCLAIMER = "AI answers can be wrong. Before using any pesticide, medicine or dose, "
            + "check with your local agriculture officer or vet.";

    public static QuestionAiAnswerResponse from(QuestionAiAnswer aiAnswer) {
        return new QuestionAiAnswerResponse(aiAnswer.getId(), aiAnswer.getQuestion().getId(), aiAnswer.getAnswer(),
                List.copyOf(aiAnswer.getSources()), aiAnswer.getStatus(), DISCLAIMER,
                aiAnswer.getReviewedBy() == null ? null : aiAnswer.getReviewedBy().getName(),
                CommonUtils.toInstant(aiAnswer.getReviewedAt()),
                aiAnswer.getCorrection() == null ? null : aiAnswer.getCorrection().getId(),
                CommonUtils.toInstant(aiAnswer.getCreationDate()));
    }
}
