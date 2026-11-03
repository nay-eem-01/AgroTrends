package com.project.agriculturalblogapplication.model.response;

import java.time.LocalDate;

/**
 * One Monday-to-Sunday week (Asia/Dhaka) of the launch test (PLAN §6). {@code staffActivity} is the same row counting
 * only what staff did (null inside it), so the team sees how much of the activity is their own.
 */
public record LaunchWeekResponse(
        LocalDate weekStart,
        boolean partial,
        long activeAskers,
        long activeAnswerers,
        long activeMembers,
        long questions,
        long humanAnsweredWithin24h,
        long aiAnswers,
        long aiReviewedWithin48h,
        long verifiedExperts,
        long newMembers,
        LaunchWeekResponse staffActivity) {
}
