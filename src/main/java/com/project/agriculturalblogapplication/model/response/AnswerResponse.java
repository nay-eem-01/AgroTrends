package com.project.agriculturalblogapplication.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.project.agriculturalblogapplication.enums.Badge;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AnswerResponse {
    private Long answerId;
    private Long questionId;
    private Long userId;
    private String content;

    /** The author's display name (never their e-mail). */
    private String authorName;

    /** The author's total reputation. */
    private long authorReputation;

    /** The author's badges in this question's topic. */
    private List<Badge> authorBadges;

    private Instant createdAt;

    private Instant updatedAt;

    /** Upvotes minus downvotes. */
    private long score;

    /** True for the answer the asker accepted. */
    private boolean accepted;
}
