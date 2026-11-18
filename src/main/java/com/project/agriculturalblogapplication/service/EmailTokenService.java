package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.EmailToken;
import com.project.agriculturalblogapplication.entities.EmailToken.Purpose;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.EmailTokenRepository;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import lombok.RequiredArgsConstructor;
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

/** Issues and checks the single-use tokens behind e-mailed links. One live token per user and purpose. */
@Service
@RequiredArgsConstructor
public class EmailTokenService {

    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);
    private static final int MAX_REQUESTS_PER_EMAIL = 3;
    private static final int MAX_REQUESTS_PER_IP = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    private final EmailTokenRepository tokenRepository;
    private final AttemptLimiter attemptLimiter;

    /**
     * Rate-limits requests for an e-mailed link (429 when over the limit). Call it before looking the account up:
     * every request counts, known account or not, so the limit reveals nothing.
     */
    public void limitRequests(Purpose purpose, String email, String clientIp, String lang) {
        String emailKey = purpose + ":email:" + email;
        String ipKey = purpose + ":ip:" + clientIp;
        if (attemptLimiter.isBlocked(emailKey) || attemptLimiter.isBlocked(ipKey)) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ERROR_TOO_MANY_ATTEMPTS, lang);
        }
        attemptLimiter.record(emailKey, MAX_REQUESTS_PER_EMAIL, REQUEST_WINDOW);
        attemptLimiter.record(ipKey, MAX_REQUESTS_PER_IP, REQUEST_WINDOW);
    }

    /** A new random token for the user, replacing any earlier one for the same purpose. Returns the raw token. */
    public String issue(Long userId, Purpose purpose) {
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        tokenRepository.deleteAllByUserIdAndPurpose(userId, purpose);
        tokenRepository.save(new EmailToken(userId, purpose, hash(token), Instant.now().plus(purpose.getLifetime())));
        return token;
    }

    /** The user a valid token belongs to; 400 for an unknown, expired or wrong-purpose token. Does not use it up. */
    public Long userIdFor(String token, Purpose purpose, String lang) {
        EmailToken stored = tokenRepository.findByTokenHashAndPurpose(hash(token), purpose).orElseThrow(() ->
                new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_OR_EXPIRED_LINK, lang));
        if (stored.getExpiryDate().isBefore(Instant.now())) {
            revoke(stored.getUserId(), purpose);
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_OR_EXPIRED_LINK, lang);
        }
        return stored.getUserId();
    }

    public void revoke(Long userId, Purpose purpose) {
        tokenRepository.deleteAllByUserIdAndPurpose(userId, purpose);
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
