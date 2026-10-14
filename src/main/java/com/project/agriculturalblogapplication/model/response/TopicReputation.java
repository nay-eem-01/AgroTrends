package com.project.agriculturalblogapplication.model.response;

/** Points earned in one topic; {@code categoryId} is null for questions asked before topics existed. */
public record TopicReputation(Long categoryId, String categoryName, long points) {
}
