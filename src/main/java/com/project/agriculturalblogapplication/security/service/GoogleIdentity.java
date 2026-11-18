package com.project.agriculturalblogapplication.security.service;

/** Who a verified Google ID token says the user is. {@code toString} leaves the e-mail out of logs. */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {

    @Override
    public String toString() {
        return "GoogleIdentity[subject=" + subject + "]";
    }
}
