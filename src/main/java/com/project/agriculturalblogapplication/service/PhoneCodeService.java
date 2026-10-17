package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.PhoneCode;
import com.project.agriculturalblogapplication.entities.PhoneCode.Purpose;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.PhoneCodeRepository;
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
import java.util.HexFormat;

/** Issues and checks the 6-digit codes sent by SMS. One live code per user and purpose. */
@Service
@RequiredArgsConstructor
public class PhoneCodeService {

    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);
    private static final int MAX_REQUESTS_PER_PHONE = 3;
    private static final int MAX_REQUESTS_PER_IP = 10;
    private static final Duration CHECK_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_CHECKS_PER_IP = 20;

    private final SecureRandom secureRandom = new SecureRandom();

    private final PhoneCodeRepository codeRepository;
    private final AttemptLimiter attemptLimiter;

    /**
     * Rate-limits requests for a code (429 when over the limit); SMS cost money. Call it before looking the account
     * up: every request counts, known number or not, so the limit reveals nothing.
     */
    public void limitRequests(Purpose purpose, String phone, String clientIp, String lang) {
        String phoneKey = "sms:" + purpose + ":phone:" + phone;
        String ipKey = "sms:" + purpose + ":ip:" + clientIp;
        if (attemptLimiter.isBlocked(phoneKey) || attemptLimiter.isBlocked(ipKey)) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ERROR_TOO_MANY_ATTEMPTS, lang);
        }
        attemptLimiter.record(phoneKey, MAX_REQUESTS_PER_PHONE, REQUEST_WINDOW);
        attemptLimiter.record(ipKey, MAX_REQUESTS_PER_IP, REQUEST_WINDOW);
    }

    /** Rate-limits code checks per network, on top of the per-code attempt limit. */
    public void limitChecks(String clientIp, String lang) {
        String ipKey = "sms-check:ip:" + clientIp;
        if (attemptLimiter.isBlocked(ipKey)) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ERROR_TOO_MANY_ATTEMPTS, lang);
        }
        attemptLimiter.record(ipKey, MAX_CHECKS_PER_IP, CHECK_WINDOW);
    }

    /** A new random code for the user, replacing any earlier one for the same purpose. Returns the raw code. */
    public String issue(Long userId, Purpose purpose) {
        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        codeRepository.deleteAllByUserIdAndPurpose(userId, purpose);
        codeRepository.save(new PhoneCode(userId, purpose, hash(userId, code), Instant.now().plus(purpose.getLifetime())));
        return code;
    }

    /**
     * Uses up the user's code when it matches; otherwise 400, and the code dies after its last allowed attempt.
     * {@code userId} may be null (no account for the number): that is the same 400.
     */
    public void consume(Long userId, Purpose purpose, String code, String lang) {
        PhoneCode stored = userId == null ? null : codeRepository.findByUserIdAndPurpose(userId, purpose).orElse(null);
        if (stored == null || stored.getExpiryDate().isBefore(Instant.now())) {
            if (stored != null) {
                revoke(userId, purpose);
            }
            throw invalid(lang);
        }
        if (!MessageDigest.isEqual(stored.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(userId, code).getBytes(StandardCharsets.UTF_8))) {
            stored.setFailedAttempts(stored.getFailedAttempts() + 1);
            if (stored.getFailedAttempts() >= PhoneCode.MAX_ATTEMPTS) {
                revoke(userId, purpose);
            } else {
                codeRepository.save(stored);
            }
            throw invalid(lang);
        }
        revoke(userId, purpose);
    }

    public void revoke(Long userId, Purpose purpose) {
        codeRepository.deleteAllByUserIdAndPurpose(userId, purpose);
    }

    private static ApplicationException invalid(String lang) {
        return new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_OR_EXPIRED_CODE, lang);
    }

    // Salted with the user id, so equal codes of different users have different hashes.
    private static String hash(Long userId, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((userId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
