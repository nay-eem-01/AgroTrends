package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository, mock(PasswordEncoder.class),
            mock(RoleService.class), mock(AuthorService.class), mock(AuthorizationService.class),
            mock(RefreshTokenService.class), mock(UserSessionService.class), mock(AttemptLimiter.class));

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
