package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.enums.VoteDirection;

/** A post's score after the vote and the caller's vote on it (null when they have none). */
public record VoteResponse(long score, VoteDirection myVote) {
}
