package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.UpdateUserRequest;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final UserSessionService sessions = mock(UserSessionService.class);
    private final UserService userService = new UserService(userRepository, mock(PasswordEncoder.class),
            mock(RoleService.class), mock(AuthorService.class), authorization,
            mock(RefreshTokenService.class), sessions, mock(AttemptLimiter.class), events);

    private User signedIn(String email, String mobileNumber) {
        User user = new User();
        user.setId(3L);
        user.setEmail(email);
        user.setMobileNumber(mobileNumber);
        user.setEmailVerifiedAt(email == null ? null : LocalDateTime.now());
        user.setPhoneVerifiedAt(mobileNumber == null ? null : LocalDateTime.now());
        when(authorization.currentUserId("en")).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return user;
    }

    private static UpdateUserRequest update(String email, String mobileNumber) {
        return new UpdateUserRequest("Rahim", email, "+880", mobileNumber);
    }

    @Test
    void addingAPhoneLaterKeepsTheConfirmedEmailAndAsksToConfirmThePhone() {
        User user = signedIn("rahim@gmail.com", null);

        userService.update(update("rahim@gmail.com", "01712345678"), "en");

        assertEquals("+8801712345678", user.getMobileNumber());
        assertNull(user.getPhoneVerifiedAt());
        assertNotNull(user.getEmailVerifiedAt());
        verify(events).publishEvent(new ContactChangedEvent(3L, false, true));
        verify(sessions, never()).deactivatePreviousSession(any());
    }

    @Test
    void addingAnEmailLaterToAPhoneAccountAsksToConfirmIt() {
        User user = signedIn(null, "+8801712345678");

        userService.update(update("Rahim@Gmail.com", "+8801712345678"), "en");

        assertEquals("Rahim@Gmail.com", user.getEmail());
        assertNotNull(user.getPhoneVerifiedAt());
        verify(events).publishEvent(new ContactChangedEvent(3L, true, false));
    }

    @Test
    void theEmailCanBeRemovedWhileAPhoneRemains() {
        User user = signedIn("rahim@gmail.com", "+8801712345678");

        userService.update(update(null, "01712345678"), "en");

        assertNull(user.getEmail());
        assertNull(user.getEmailVerifiedAt());
        assertNotNull(user.getPhoneVerifiedAt());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void removingBothIsA400() {
        signedIn("rahim@gmail.com", "+8801712345678");

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApplicationException.class,
                () -> userService.update(update(" ", null), "en")).getHttpStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void aBannedAccountsPhoneGetsTheBanAtSignUp() {
        User banned = new User();
        banned.setBannedAt(LocalDateTime.now());
        when(userRepository.findTopByMobileNumber("+8801712345678")).thenReturn(Optional.of(banned));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApplicationException.class,
                () -> userService.validateMobile("+8801712345678", "en")).getHttpStatus());
    }

    @Test
    void aBannedAccountsEmailGetsTheBanAtSignUpAndAtTheEmailPrecheck() {
        User banned = new User();
        banned.setBannedAt(LocalDateTime.now());
        when(userRepository.findTopByEmailEqualsIgnoreCase("rahim@gmail.com")).thenReturn(Optional.of(banned));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApplicationException.class,
                () -> userService.validateEmail("rahim@gmail.com", "en")).getHttpStatus());
    }

    @Test
    void anActiveAccountsEmailIsStillAlreadyRegistered() {
        when(userRepository.findTopByEmailEqualsIgnoreCase("karim@gmail.com")).thenReturn(Optional.of(new User()));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApplicationException.class,
                () -> userService.validateEmail("karim@gmail.com", "en")).getHttpStatus());
    }
}
