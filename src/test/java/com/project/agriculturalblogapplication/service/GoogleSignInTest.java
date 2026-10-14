package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.RefreshToken;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.ChangePasswordRequest;
import com.project.agriculturalblogapplication.model.request.GoogleSignInRequest;
import com.project.agriculturalblogapplication.model.response.WebTokenResponse;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.jwt.JwtUtil;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.GoogleIdTokenVerifier;
import com.project.agriculturalblogapplication.security.service.GoogleIdentity;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import com.project.agriculturalblogapplication.model.request.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Sign in with Google: which account a Google identity lands in, and what linking does to an existing one. */
class GoogleSignInTest {

    private static final String SUBJECT = "google-123";
    private static final String EMAIL = "rahim@gmail.com";

    private final GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
    private final UserService userService = mock(UserService.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final AuthService authService = new AuthService(jwtUtil, refreshTokenService, mock(AuthenticationManager.class),
            userService, mock(UserSessionService.class), mock(AttemptLimiter.class), verifier, mock(EmailVerificationService.class));

    @BeforeEach
    void setUp() {
        when(verifier.verify("id-token", "en")).thenReturn(new GoogleIdentity(SUBJECT, EMAIL, true, "Rahim"));
        when(jwtUtil.generateAccessToken(anyString())).thenReturn("access");
        RefreshToken refresh = new RefreshToken();
        refresh.setToken("refresh");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refresh);
        when(userService.findByGoogleSubject(SUBJECT)).thenReturn(Optional.empty());
    }

    @Test
    void unknownGoogleAccountCreatesAnAccountWithTheRequestedType() {
        User created = user(RoleType.USER, null);
        when(userService.createGoogleUser(any(), any())).thenReturn(created);

        WebTokenResponse tokens = authService.signInWithGoogle(request(), "en");

        assertEquals("access", tokens.getAccessToken());
        verify(userService).createGoogleUser(new GoogleIdentity(SUBJECT, EMAIL, true, "Rahim"), "en");
        verify(userService, never()).linkGoogle(any(), any());
    }

    @Test
    void aBannedAccountsEmailCannotSignUpAgain() {
        User banned = user(RoleType.USER, null);
        banned.setBannedAt(java.time.LocalDateTime.now());
        when(userService.findByEmail(EMAIL)).thenReturn(banned);
        SignUpRequest request = new SignUpRequest();
        request.setEmail(EMAIL);
        request.setName("Rahim");
        request.setCountryCode("+880");
        request.setMobileNumber("1711111111");
        request.setPassword("Passw0rd!");

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN,
                assertThrows(ApplicationException.class, () -> authService.signUp(request, "en")).getHttpStatus());
        verify(userService, never()).createUser(any(), any());
    }

    @Test
    void aBannedAccountGetsNoTokens() {
        User banned = user(RoleType.USER, SUBJECT);
        banned.setBannedAt(java.time.LocalDateTime.now());
        when(userService.findByGoogleSubject(SUBJECT)).thenReturn(Optional.of(banned));

        ApplicationException e = assertThrows(ApplicationException.class, () -> authService.signInWithGoogle(request(), "en"));

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, e.getHttpStatus());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void linkedGoogleAccountSignsStraightIn() {
        User linked = user(RoleType.USER, SUBJECT);
        when(userService.findByGoogleSubject(SUBJECT)).thenReturn(Optional.of(linked));

        assertNotNull(authService.signInWithGoogle(request(), "en"));

        verify(userService, never()).linkGoogle(any(), any());
        verify(userService, never()).createGoogleUser(any(), any());
    }

    @Test
    void existingAccountWithTheSameEmailIsLinked() {
        User existing = user(RoleType.USER, null);
        when(userService.findByEmail(EMAIL)).thenReturn(existing);
        when(userService.linkGoogle(existing, SUBJECT)).thenReturn(existing);

        authService.signInWithGoogle(request(), "en");

        verify(userService).linkGoogle(existing, SUBJECT);
        verify(userService, never()).createGoogleUser(any(), any());
    }

    @Test
    void adminAccountIsRefusedAndLeftUntouched() {
        when(userService.findByEmail(EMAIL)).thenReturn(user(RoleType.SUPER_ADMIN, null));

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> authService.signInWithGoogle(request(), "en"));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getHttpStatus());
        verify(userService, never()).linkGoogle(any(), any());
        verify(jwtUtil, never()).generateAccessToken(anyString());
    }

    @Test
    void unverifiedGoogleEmailIsRefusedBeforeAnyLookup() {
        when(verifier.verify("id-token", "en")).thenReturn(new GoogleIdentity(SUBJECT, EMAIL, false, "Rahim"));

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> authService.signInWithGoogle(request(), "en"));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getHttpStatus());
        verify(userService, never()).findByEmail(any());
    }

    @Test
    void linkingAnUnverifiedAccountDropsItsPasswordAndSessions() {
        // Whoever registered an e-mail they never verified may not own it: only the Google owner keeps access.
        UserRepository repository = mock(UserRepository.class);
        UserSessionService sessions = mock(UserSessionService.class);
        RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
        UserService service = userService(repository, sessions, refreshTokens);
        User existing = user(RoleType.USER, null);
        when(repository.save(existing)).thenReturn(existing);

        service.linkGoogle(existing, SUBJECT);

        assertNull(existing.getPassword());
        assertEquals(SUBJECT, existing.getGoogleSubject());
        assertNotNull(existing.getEmailVerifiedAt());
        verify(sessions).deactivatePreviousSession(10L);
        verify(refreshTokens).deleteAllByUserId(10L);
    }

    @Test
    void linkingAVerifiedAccountKeepsItsPasswordAndSessions() {
        UserRepository repository = mock(UserRepository.class);
        UserSessionService sessions = mock(UserSessionService.class);
        UserService service = userService(repository, sessions, mock(RefreshTokenService.class));
        User existing = user(RoleType.USER, null);
        existing.setEmailVerifiedAt(LocalDateTime.now().minusDays(3));
        when(repository.save(existing)).thenReturn(existing);

        service.linkGoogle(existing, SUBJECT);

        assertEquals("$2a$10$hash", existing.getPassword());
        verify(sessions, never()).deactivatePreviousSession(any());
    }

    @Test
    void googleOnlyAccountGetsA409FromChangePasswordInsteadOfWrongPassword() {
        UserRepository repository = mock(UserRepository.class);
        AuthorizationService authorization = mock(AuthorizationService.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class), mock(RoleService.class),
                mock(AuthorService.class), authorization, mock(RefreshTokenService.class), mock(UserSessionService.class),
                mock(AttemptLimiter.class));
        User googleOnly = user(RoleType.USER, SUBJECT);
        googleOnly.setPassword(null);
        UserService spy = spy(service);
        doReturn(googleOnly).when(spy).getUserInfo("en");
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("anything");
        request.setNewPassword("NewPassw0rd!");

        ApplicationException e = assertThrows(ApplicationException.class, () -> spy.changePassword(request, "en"));

        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
    }

    private static UserService userService(UserRepository repository, UserSessionService sessions, RefreshTokenService refreshTokens) {
        return new UserService(repository, mock(PasswordEncoder.class), mock(RoleService.class), mock(AuthorService.class),
                mock(AuthorizationService.class), refreshTokens, sessions, mock(AttemptLimiter.class));
    }

    private static GoogleSignInRequest request() {
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setIdToken("id-token");
        return request;
    }

    private static User user(RoleType roleType, String googleSubject) {
        Role role = new Role();
        role.setRoleType(roleType);
        role.setRoleName(roleType.name());
        User user = new User();
        user.setId(10L);
        user.setName("Rahim");
        user.setEmail(EMAIL);
        user.setPassword("$2a$10$hash");
        user.setGoogleSubject(googleSubject);
        user.setRoles(Set.of(role));
        return user;
    }
}
