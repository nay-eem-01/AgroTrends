package com.project.agriculturalblogapplication.enums;

/** Why something was reported. Stored as text without a CHECK constraint, so reasons can be added freely. */
public enum ReportReason {
    /** Wrong or dangerous farming advice, e.g. a pesticide dose. */
    HARMFUL_ADVICE,
    SPAM,
    ABUSE,
    FALSE_CREDENTIALS,
    OTHER
}
