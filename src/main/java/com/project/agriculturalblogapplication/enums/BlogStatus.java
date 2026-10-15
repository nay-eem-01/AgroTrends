package com.project.agriculturalblogapplication.enums;

public enum BlogStatus {
    /** Visible to its author only; not in lists, search or AI answers. */
    DRAFT,
    PUBLISHED,
    /** Reported often enough to be taken down until a moderator decides: visible to nobody, not even the author. */
    HIDDEN
}
