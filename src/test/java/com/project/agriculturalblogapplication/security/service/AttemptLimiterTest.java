package com.project.agriculturalblogapplication.security.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttemptLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final AttemptLimiter limiter = new AttemptLimiter(now::get);
    private final Duration window = Duration.ofMinutes(15);

    @Test
    void blocksOnlyAfterMaxAttempts() {
        for (int i = 0; i < 4; i++) {
            limiter.record("k", 5, window);
            assertFalse(limiter.isBlocked("k"));
        }
        limiter.record("k", 5, window);
        assertTrue(limiter.isBlocked("k"));
    }

    @Test
    void unblocksWhenWindowEnds() {
        for (int i = 0; i < 5; i++) {
            limiter.record("k", 5, window);
        }
        assertTrue(limiter.isBlocked("k"));

        now.addAndGet(window.toMillis() + 1);
        assertFalse(limiter.isBlocked("k"));
    }

    @Test
    void resetClearsBlockAndKeysAreIndependent() {
        for (int i = 0; i < 5; i++) {
            limiter.record("a", 5, window);
        }
        assertFalse(limiter.isBlocked("b"));

        limiter.reset("a");
        assertFalse(limiter.isBlocked("a"));
    }
}
