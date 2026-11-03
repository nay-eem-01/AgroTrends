package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.MailKind;
import com.project.agriculturalblogapplication.model.request.UpdateUserSettingsRequest;
import com.project.agriculturalblogapplication.model.response.UserSettingsResponse;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.UnsubscribeTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserSettingsServiceTest {

    private final UserService userService = mock(UserService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final UnsubscribeTokens tokens = new UnsubscribeTokens("a-test-secret-that-is-at-least-32-characters");
    private final UserSettingsService settings = new UserSettingsService(userService, users, authorization, tokens);

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        when(authorization.currentUserId("en")).thenReturn(7L);
        when(userService.findByIdWithException(7L, "en")).thenReturn(user);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void newAccountsGetBanglaAndBothEmails() {
        assertEquals(new UserSettingsResponse("bn", true, true), settings.mine("en"));
    }

    @Test
    void anUpdateChangesOnlyTheFieldsItNames() {
        UserSettingsResponse updated = settings.update(new UpdateUserSettingsRequest("en", null, false), "en");

        assertEquals(new UserSettingsResponse("en", true, false), updated);
    }

    @Test
    void anUnsubscribeLinkSwitchesOffOnlyItsKind() {
        assertTrue(settings.unsubscribe(tokens.tokenFor(7L, MailKind.digest), MailKind.digest));

        assertTrue(user.isEmailOnAnswer());
        assertFalse(user.isEmailWeeklyDigest());
        assertFalse(settings.unsubscribe("7.forged", MailKind.answer));
        assertTrue(user.isEmailOnAnswer());
    }
}
