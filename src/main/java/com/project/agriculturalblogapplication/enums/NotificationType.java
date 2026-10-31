package com.project.agriculturalblogapplication.enums;

/** Why a user is being notified. Stored as text without a CHECK constraint, so new types need no schema change. */
public enum NotificationType {
    /** Someone answered your question. */
    ANSWER_ON_YOUR_QUESTION,
    /** The asker accepted your answer. */
    ANSWER_ACCEPTED,
    /** An expert confirmed, corrected or flagged the AI answer on your question. */
    AI_ANSWER_REVIEWED,
    /** A new AI answer is waiting for review in one of your expert topics. */
    NEW_REVIEW_ITEM,
    /** A new question in one of your expert topics has no AI answer (the AI failed or the asker hit the limit). */
    NEW_QUESTION_IN_TOPIC,
    /** Your expert application was approved or rejected. */
    APPLICATION_DECIDED,
    /** Someone replied to your comment. */
    COMMENT_REPLY,
    /** A moderator removed your post after a report. */
    CONTENT_REMOVED,
    /** Several users reported your post: it is hidden until a moderator reviews it. */
    CONTENT_HIDDEN,
    /** A moderator found the reports unfounded: your post is visible again. */
    CONTENT_RESTORED
}
