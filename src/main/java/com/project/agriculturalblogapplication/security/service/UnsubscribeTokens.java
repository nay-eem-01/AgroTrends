package com.project.agriculturalblogapplication.security.service;

import com.project.agriculturalblogapplication.enums.MailKind;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

/**
 * One-click unsubscribe tokens: "{userId}.{HMAC of userId and kind}". They carry no password and never expire, so a
 * link in an old e-mail still works; the key is derived from the JWT secret, so rotating it voids old links.
 */
@Component
public class UnsubscribeTokens {

    private final SecretKeySpec key;

    public UnsubscribeTokens(@Value("${app.jwt.secret}") String jwtSecret) {
        this.key = new SecretKeySpec(("unsubscribe:" + jwtSecret).getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String tokenFor(Long userId, MailKind kind) {
        return userId + "." + signature(userId, kind);
    }

    /** The user the token was made for, or empty when it was not made by us for this kind. */
    public Optional<Long> userIdOf(String token, MailKind kind) {
        int dot = token == null ? -1 : token.indexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        try {
            Long userId = Long.valueOf(token.substring(0, dot));
            boolean valid = MessageDigest.isEqual(signature(userId, kind).getBytes(StandardCharsets.UTF_8),
                    token.substring(dot + 1).getBytes(StandardCharsets.UTF_8));
            return valid ? Optional.of(userId) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String signature(Long userId, MailKind kind) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal((userId + ":" + kind.name()).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
