package com.project.agriculturalblogapplication.model;

import com.project.agriculturalblogapplication.enums.Badge;

import java.util.List;
import java.util.Set;

/** What a post shows about its author: total reputation and the topics they are a Top Contributor in. */
public record AuthorStanding(long reputation, Set<Long> topContributorTopics) {

    public static final AuthorStanding NONE = new AuthorStanding(0, Set.of());

    /** The author's badges in the topic of the post being shown. */
    public List<Badge> badgesIn(Long topicId) {
        return topicId != null && topContributorTopics.contains(topicId) ? List.of(Badge.TOP_CONTRIBUTOR) : List.of();
    }
}
