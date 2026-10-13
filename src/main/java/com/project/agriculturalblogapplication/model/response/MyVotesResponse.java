package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.enums.VoteDirection;

import java.util.Map;

/** The caller's votes on a question (null when none) and on its answers, keyed by answer id. */
public record MyVotesResponse(VoteDirection question, Map<Long, VoteDirection> answers) {
}
