package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateUserRequest;
import com.project.agriculturalblogapplication.model.request.SignUpRequest;
import com.project.agriculturalblogapplication.security.jwt.JwtUtil;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import com.project.agriculturalblogapplication.security.service.GoogleIdTokenVerifier;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Sign up with an e-mail, a phone number or both. */
class SignUpTest {

    private final UserService userService = mock(UserService.class);
    private final AuthService authService = new AuthService(mock(JwtUtil.class), mock(RefreshTokenService.class),
            mock(AuthenticationManager.class), userService, mock(UserSessionService.class), mock(AttemptLimiter.class),
            mock(GoogleIdTokenVerifier.class));

    @BeforeEach
    void setUp() {
        when(userService.createUser(any(), any())).thenReturn(new User());
        when(userService.mobileOrNull(any(), any(), any())).thenCallRealMethod();
    }

    private static SignUpRequest request(String email, String countryCode, String mobileNumber) {
        SignUpRequest request = new SignUpRequest();
        request.setName("Rahim");
        request.setEmail(email);
        request.setCountryCode(countryCode);
        request.setMobileNumber(mobileNumber);
        request.setPassword("Passw0rd!");
        return request;
    }

    private CreateUserRequest created() {
        ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
        verify(userService).createUser(captor.capture(), any());
        return captor.getValue();
    }

    @Test
    void phoneOnlyCreatesAnAccountWithoutEmail() {
        authService.signUp(request(null, "+880", "01712345678"), "en");

        assertNull(created().getEmail());
        assertEquals("+8801712345678", created().getMobileNumber());
        verify(userService).validateMobile("+8801712345678", "en");
        verify(userService, never()).validateEmail(any(), any());
    }

    @Test
    void emailOnlyCreatesAnAccountWithoutPhone() {
        authService.signUp(request("rahim@example.com", "+880", " "), "en");

        assertEquals("rahim@example.com", created().getEmail());
        assertNull(created().getMobileNumber());
        verify(userService, never()).validateMobile(any(), any());
    }

    @Test
    void bothAreCheckedAndKept() {
        authService.signUp(request("rahim@example.com", null, "1712345678"), "en");

        assertEquals("rahim@example.com", created().getEmail());
        assertEquals("+8801712345678", created().getMobileNumber());
        verify(userService).validateEmail("rahim@example.com", "en");
        verify(userService).validateMobile("+8801712345678", "en");
    }

    @Test
    void neitherIsA400() {
        ApplicationException e = assertThrows(ApplicationException.class,
                () -> authService.signUp(request("", "+880", null), "en"));

        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        verify(userService, never()).createUser(any(), any());
    }

    @Test
    void aNumberThatIsNotAPhoneIsA400() {
        ApplicationException e = assertThrows(ApplicationException.class,
                () -> authService.signUp(request("rahim@example.com", "+880", "12ab"), "en"));

        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        verify(userService, never()).createUser(any(), any());
    }
}
