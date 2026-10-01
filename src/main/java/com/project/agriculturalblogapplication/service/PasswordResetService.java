package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.PasswordResetToken;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.mail.MailService;
import com.project.agriculturalblogapplication.repositories.PasswordResetTokenRepository;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * E-mail based password reset. The e-mailed token is random, single-use and valid for 15 minutes; only its
 * SHA-256 hash is stored. Requesting a reset never reveals whether the e-mail has an account.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);
    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);
    private static final int MAX_REQUESTS_PER_EMAIL = 3;
    private static final int MAX_REQUESTS_PER_IP = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final MailService mailService;
    private final AttemptLimiter attemptLimiter;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    public void requestReset(String rawEmail, String clientIp, String lang) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String emailKey = "forgot-password:email:" + email;
        String ipKey = "forgot-password:ip:" + clientIp;

        if (attemptLimiter.isBlocked(emailKey) || attemptLimiter.isBlocked(ipKey)) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ERROR_TOO_MANY_ATTEMPTS, lang);
        }
        // Count every request, known account or not, so the limit leaks nothing.
        attemptLimiter.record(emailKey, MAX_REQUESTS_PER_EMAIL, REQUEST_WINDOW);
        attemptLimiter.record(ipKey, MAX_REQUESTS_PER_IP, REQUEST_WINDOW);

        User user = userService.findByEmail(email);
        if (user == null) {
            return;
        }

        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        tokenRepository.deleteAllByUserId(user.getId());
        PasswordResetToken entity = new PasswordResetToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(hash(token));
        entity.setExpiryDate(Instant.now().plus(TOKEN_LIFETIME));
        tokenRepository.save(entity);

        mailService.sendPasswordReset(user.getEmail(), frontendUrl + "/reset-password?token=" + token);
    }

    public void reset(String token, String newPassword, String lang) {
        PasswordResetToken stored = tokenRepository.findByTokenHash(hash(token)).orElseThrow(() ->
                new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_OR_EXPIRED_RESET_TOKEN, lang));

        if (stored.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.deleteAllByUserId(stored.getUserId());
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_OR_EXPIRED_RESET_TOKEN, lang);
        }

        User user = userService.findByIdWithException(stored.getUserId(), lang);
        // Validates the password policy first, so a weak password does not burn the token.
        userService.setNewPassword(user, newPassword, lang);
        tokenRepository.deleteAllByUserId(user.getId());
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
