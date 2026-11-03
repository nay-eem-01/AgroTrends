package com.project.agriculturalblogapplication.model.request;

import jakarta.validation.constraints.Pattern;

/** Partial update: a null field keeps its current value. */
public record UpdateUserSettingsRequest(
        @Pattern(regexp = "en|bn", message = "preferredLang must be en or bn") String preferredLang,
        Boolean emailOnAnswer,
        Boolean emailWeeklyDigest) {
}
