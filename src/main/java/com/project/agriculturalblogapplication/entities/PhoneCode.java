package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

/**
 * A 6-digit code sent by SMS (phone confirmation, password reset). Only its SHA-256 hash is stored; a code dies after
 * {@link #MAX_ATTEMPTS} wrong guesses, so it cannot be brute-forced.
 */
@Entity
@Table(name = "phone_code")
@Getter
@Setter
@NoArgsConstructor
public class PhoneCode extends AuditModel<String> {

    public static final int MAX_ATTEMPTS = 5;

    @Getter
    @RequiredArgsConstructor
    public enum Purpose {
        VERIFY_PHONE(Duration.ofMinutes(10)),
        RESET_PASSWORD(Duration.ofMinutes(10));

        private final Duration lifetime;
    }

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private Purpose purpose;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    public PhoneCode(Long userId, Purpose purpose, String codeHash, Instant expiryDate) {
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiryDate = expiryDate;
    }
}
