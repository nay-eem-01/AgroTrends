package com.project.agriculturalblogapplication.security.service;

import com.project.agriculturalblogapplication.enums.MailKind;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnsubscribeTokensTest {

    private final UnsubscribeTokens tokens = new UnsubscribeTokens("a-test-secret-that-is-at-least-32-characters");

    @Test
    void aTokenNamesItsUserForItsKindOnly() {
        String token = tokens.tokenFor(7L, MailKind.answer);

        assertEquals(Optional.of(7L), tokens.userIdOf(token, MailKind.answer));
        assertEquals(Optional.empty(), tokens.userIdOf(token, MailKind.digest));
    }

    @Test
    void aTokenForAnotherUserOrAMadeUpOneIsRefused() {
        String forSeven = tokens.tokenFor(7L, MailKind.answer);

        assertEquals(Optional.empty(), tokens.userIdOf("8" + forSeven.substring(1), MailKind.answer));
        assertEquals(Optional.empty(), tokens.userIdOf("7.not-a-signature", MailKind.answer));
        assertEquals(Optional.empty(), tokens.userIdOf("garbage", MailKind.answer));
        assertEquals(Optional.empty(), tokens.userIdOf(null, MailKind.answer));
        assertEquals(Optional.empty(), new UnsubscribeTokens("another-secret-that-is-at-least-32-chars")
                .userIdOf(forSeven, MailKind.answer));
    }
}
