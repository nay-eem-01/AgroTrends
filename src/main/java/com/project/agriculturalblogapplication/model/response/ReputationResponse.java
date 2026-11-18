package com.project.agriculturalblogapplication.model.response;

import java.util.List;

/** A user's reputation: the total and its split by topic, highest first. */
public record ReputationResponse(long total, List<TopicReputation> topics) {
}
