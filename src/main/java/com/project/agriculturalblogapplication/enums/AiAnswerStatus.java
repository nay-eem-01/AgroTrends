package com.project.agriculturalblogapplication.enums;

/** Where an AI answer on a question is in expert review (roadmap 7.3, 7.4). */
public enum AiAnswerStatus {
    UNCHECKED,
    CONFIRMED,
    /** An expert posted a corrected answer instead. */
    CORRECTED,
    /** Hidden: an expert or Top Contributor found it wrong or unsafe. */
    FLAGGED
}
