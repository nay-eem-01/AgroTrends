package com.project.agriculturalblogapplication.model.response;

/** The caller's own settings: e-mail language and which e-mails they want. */
public record UserSettingsResponse(String preferredLang, boolean emailOnAnswer, boolean emailWeeklyDigest) {
}
