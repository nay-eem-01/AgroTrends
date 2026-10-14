package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.EmailToken;
import com.project.agriculturalblogapplication.entities.EmailToken.Purpose;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.EmailTokenRepository;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailTokenServiceTest {

    private final EmailTokenRepository tokens = mock(EmailTokenRepository.class);
    private final AttemptLimiter limiter = mock(AttemptLimiter.class);
    private final EmailTokenService service = new EmailTokenService(tokens, limiter);

    @Test
    void issuedTokenIsStoredOnlyAsAHashWithThePurposesLifetime() {
        String token = service.issue(7L, Purpose.VERIFY_EMAIL);

        ArgumentCaptor<EmailToken> saved = ArgumentCaptor.forClass(EmailToken.class);
        verify(tokens).deleteAllByUserIdAndPurpose(7L, Purpose.VERIFY_EMAIL);
        verify(tokens).save(saved.capture());
        assertNotEquals(token, saved.getValue().getTokenHash());
        assertEquals(Purpose.VERIFY_EMAIL, saved.getValue().getPurpose());
        Duration lifetime = Duration.between(Instant.now(), saved.getValue().getExpiryDate());
        assertTrue(lifetime.compareTo(Duration.ofHours(23)) > 0 && lifetime.compareTo(Duration.ofHours(24)) <= 0);
    }

    @Test
    void aValidTokenResolvesToItsUser() {
        when(tokens.findByTokenHashAndPurpose(anyString(), any()))
                .thenReturn(Optional.of(stored(Instant.now().plusSeconds(60))));

        assertEquals(7L, service.userIdFor("token", Purpose.VERIFY_EMAIL, "en"));
    }

    @Test
    void unknownOrWrongPurposeTokenIsRejected() {
        when(tokens.findByTokenHashAndPurpose(anyString(), any())).thenReturn(Optional.empty());

        assertBadRequest(() -> service.userIdFor("reset-token", Purpose.VERIFY_EMAIL, "en"));
    }

    @Test
    void expiredTokenIsRejectedAndRemoved() {
        when(tokens.findByTokenHashAndPurpose(anyString(), any()))
                .thenReturn(Optional.of(stored(Instant.now().minusSeconds(1))));

        assertBadRequest(() -> service.userIdFor("token", Purpose.VERIFY_EMAIL, "en"));
        verify(tokens).deleteAllByUserIdAndPurpose(7L, Purpose.VERIFY_EMAIL);
    }

    @Test
    void tooManyRequestsAreRefused() {
        when(limiter.isBlocked("VERIFY_EMAIL:email:a@b.com")).thenReturn(true);

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> service.limitRequests(Purpose.VERIFY_EMAIL, "a@b.com", "1.2.3.4", "en"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
    }

    private static EmailToken stored(Instant expiry) {
        return new EmailToken(7L, Purpose.VERIFY_EMAIL, "hash", expiry);
    }

    private static void assertBadRequest(Runnable call) {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApplicationException.class, call::run).getHttpStatus());
    }
}
