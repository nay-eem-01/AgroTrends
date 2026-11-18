package com.project.agriculturalblogapplication.events;

/**
 * A Verified Expert badge was granted or revoked.
 * Published inside the transaction; listeners act after it commits.
 */
public record ExpertBadgeChanged(Long userId, Long topicId) {
}
