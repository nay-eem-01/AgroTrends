package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.enums.Badge;

import java.util.List;

/** Points, accepted answers and badges in one topic; {@code categoryId} is null for questions asked before topics. */
public record TopicReputation(Long categoryId, String categoryName, long points, long acceptedAnswers, List<Badge> badges) {
}
