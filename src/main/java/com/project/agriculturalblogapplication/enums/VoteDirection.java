package com.project.agriculturalblogapplication.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VoteDirection {
    UP(1),
    DOWN(-1);

    /** What the vote adds to the post's score. */
    private final int value;
}
