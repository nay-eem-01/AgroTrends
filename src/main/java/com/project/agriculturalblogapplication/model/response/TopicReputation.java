package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.enums.Badge;

import java.util.List;

/**
 * Points, accepted answers and badges in one topic; {@code categoryId} is null for questions asked before topics.
 * {@code expertField} (e.g. "Veterinarian") is set only where the user is a Verified Expert.
 */
public record TopicReputation(Long categoryId, String categoryName, long points, long acceptedAnswers, List<Badge> badges,
                              String expertField) {
}
