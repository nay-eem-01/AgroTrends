package com.project.agriculturalblogapplication.security.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Fixed-window attempt counter used to slow down credential stuffing and e-mail flooding.
 * Keys are arbitrary strings (e.g. "login:email:foo@bar.com", "login:ip:1.2.3.4"), so a lock never depends
 * on whether the account exists.
 *
 * In-memory: correct for a single instance only. Move to a shared store (Redis) before running several replicas.
 */
@Component
public class AttemptLimiter {

    private static final int EVICTION_THRESHOLD = 10_000;

    private static final class Entry {
        int count;
        long windowStart;
        long windowMillis;
        int max;
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public AttemptLimiter() {
        this(System::currentTimeMillis);
    }

    AttemptLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    /** True while the key has used up its allowance inside the current window. */
    public boolean isBlocked(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return false;
        }
        synchronized (entry) {
            return clock.getAsLong() - entry.windowStart < entry.windowMillis && entry.count >= entry.max;
        }
    }

    /** Records one attempt; once {@code max} are recorded inside {@code window}, the key stays blocked until the window ends. */
    public void record(String key, int max, Duration window) {
        long now = clock.getAsLong();
        if (entries.size() > EVICTION_THRESHOLD) {
            evictExpired(now);
        }
        Entry entry = entries.computeIfAbsent(key, k -> new Entry());
        synchronized (entry) {
            if (entry.count == 0 || now - entry.windowStart >= entry.windowMillis) {
                entry.count = 0;
                entry.windowStart = now;
            }
            entry.count++;
            entry.max = max;
            entry.windowMillis = window.toMillis();
        }
    }

    public void reset(String key) {
        entries.remove(key);
    }

    private void evictExpired(long now) {
        entries.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return now - e.getValue().windowStart >= e.getValue().windowMillis;
            }
        });
    }
}
