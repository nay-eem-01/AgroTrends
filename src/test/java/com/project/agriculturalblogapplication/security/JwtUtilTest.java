package com.project.agriculturalblogapplication.security;

import com.project.agriculturalblogapplication.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long";

    @Test
    void refusesToStartWithShortOrMissingSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short"));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null));
    }

    @Test
    void roundTripsSubjectAndType() {
        JwtUtil jwt = new JwtUtil(SECRET);
        String token = jwt.generateAccessToken("farmer@example.com");

        assertEquals("farmer@example.com", jwt.extractUsername(token));
        assertEquals("access", jwt.extractTokenType(token));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String forged = new JwtUtil("another-secret-that-is-also-32-bytes-long!!").generateAccessToken("admin@example.com");

        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(SECRET).extractUsername(forged));
    }

    @Test
    void tokensIssuedInTheSameSecondAreStillDistinct() {
        JwtUtil jwt = new JwtUtil(SECRET);
        assertNotEquals(jwt.generateAccessToken("a@b.c"), jwt.generateAccessToken("a@b.c"));
    }
}
