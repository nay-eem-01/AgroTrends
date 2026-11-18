package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.EmailToken.Purpose;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.mail.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    private final EmailTokenService tokens = mock(EmailTokenService.class);
    private final UserService users = mock(UserService.class);
    private final MailService mail = mock(MailService.class);
    private final EmailVerificationService service = new EmailVerificationService(tokens, users, mail);

    EmailVerificationServiceTest() {
        ReflectionTestUtils.setField(service, "frontendUrl", "https://agrotrends.app");
    }

    @Test
    void theLinkPointsAtTheFrontendWithTheToken() {
        when(tokens.issue(7L, Purpose.VERIFY_EMAIL)).thenReturn("tok");

        service.sendLink(user(null));

        verify(mail).sendEmailVerification("rahim@example.com", "https://agrotrends.app/verify-email?token=tok");
    }

    @Test
    void aMailFailureDoesNotFailSignUp() {
        doThrow(new RuntimeException("SMTP down")).when(mail).sendEmailVerification(anyString(), anyString());

        assertDoesNotThrow(() -> service.sendLink(user(null)));
    }

    @Test
    void verifyingConfirmsTheAddressAndUsesTheTokenUp() {
        User user = user(null);
        when(tokens.userIdFor("tok", Purpose.VERIFY_EMAIL, "en")).thenReturn(7L);
        when(users.findByIdWithException(7L, "en")).thenReturn(user);

        service.verify("tok", "en");

        assertNotNull(user.getEmailVerifiedAt());
        verify(users).saveUser(user);
        verify(tokens).revoke(7L, Purpose.VERIFY_EMAIL);
    }

    @Test
    void resendSkipsUnknownAndAlreadyConfirmedAddressesSilently() {
        when(users.findByEmail("rahim@example.com")).thenReturn(user(LocalDateTime.now()));

        service.resend(" Rahim@Example.com ", "1.2.3.4", "en");
        service.resend("nobody@example.com", "1.2.3.4", "en");

        verify(tokens).limitRequests(Purpose.VERIFY_EMAIL, "rahim@example.com", "1.2.3.4", "en");
        verify(tokens, never()).issue(any(), any());
    }

    private static User user(LocalDateTime verifiedAt) {
        User user = new User();
        user.setId(7L);
        user.setEmail("rahim@example.com");
        user.setEmailVerifiedAt(verifiedAt);
        return user;
    }
}
