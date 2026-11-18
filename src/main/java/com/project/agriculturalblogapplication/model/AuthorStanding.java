package com.project.agriculturalblogapplication.model;

import com.project.agriculturalblogapplication.enums.Badge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** What a post shows about its author: total reputation and the topics they hold a badge in. */
public record AuthorStanding(long reputation, Set<Long> topContributorTopics, Set<Long> expertTopics) {

    public static final AuthorStanding NONE = new AuthorStanding(0, Set.of(), Set.of());

    /** The author's badges in the topic of the post being shown, Verified Expert first. */
    public List<Badge> badgesIn(Long topicId) {
        List<Badge> badges = new ArrayList<>(2);
        if (topicId != null && expertTopics.contains(topicId)) {
            badges.add(Badge.VERIFIED_EXPERT);
        }
        if (topicId != null && topContributorTopics.contains(topicId)) {
            badges.add(Badge.TOP_CONTRIBUTOR);
        }
        return List.copyOf(badges);
    }
}
