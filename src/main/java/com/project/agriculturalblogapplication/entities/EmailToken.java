package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

/**
 * A single-use token sent by e-mail (password reset, e-mail confirmation). Only its SHA-256 hash is stored, so a
 * database leak cannot be used to reset passwords or confirm addresses.
 */
@Entity
@Table(name = "email_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailToken extends AuditModel<String> {

    @Getter
    @RequiredArgsConstructor
    public enum Purpose {
        RESET_PASSWORD(Duration.ofMinutes(15)),
        VERIFY_EMAIL(Duration.ofHours(24));

        private final Duration lifetime;
    }

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private Purpose purpose;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;
}
