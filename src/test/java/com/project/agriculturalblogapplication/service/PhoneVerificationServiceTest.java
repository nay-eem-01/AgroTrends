package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.PhoneCode.Purpose;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.sms.SmsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhoneVerificationServiceTest {

    private static final String PHONE = "+8801712345678";

    private final PhoneCodeService codes = mock(PhoneCodeService.class);
    private final UserService users = mock(UserService.class);
    private final SmsService sms = mock(SmsService.class);
    private final PhoneVerificationService service = new PhoneVerificationService(codes, users, sms);

    private User user(boolean verified) {
        User user = new User();
        user.setId(4L);
        user.setMobileNumber(PHONE);
        user.setPhoneVerifiedAt(verified ? LocalDateTime.now() : null);
        when(users.findByMobileNumber(PHONE)).thenReturn(user);
        when(users.findByIdWithException(4L, null)).thenReturn(user);
        return user;
    }

    @Test
    void aNewOrChangedNumberGetsACodeBySms() {
        user(false);
        when(codes.issue(4L, Purpose.VERIFY_PHONE)).thenReturn("123456");

        service.onContactChanged(new ContactChangedEvent(4L, true, true));

        verify(sms).sendVerificationCode(PHONE, "123456");
    }

    @Test
    void anEmailOnlyChangeSendsNoSms() {
        service.onContactChanged(new ContactChangedEvent(4L, true, false));

        verify(sms, never()).sendVerificationCode(any(), any());
    }

    @Test
    void theRightCodeConfirmsTheNumberGivenInLocalForm() {
        User user = user(false);

        service.verify("+880", "01712345678", "123456", "1.2.3.4", "en");

        verify(codes).consume(4L, Purpose.VERIFY_PHONE, "123456", "en");
        assertNotNull(user.getPhoneVerifiedAt());
        verify(users).saveUser(user);
    }

    @Test
    void aWrongCodeLeavesTheNumberUnconfirmed() {
        User user = user(false);
        doThrow(new ApplicationException(HttpStatus.BAD_REQUEST, "wrong"))
                .when(codes).consume(4L, Purpose.VERIFY_PHONE, "000000", "en");

        assertThrows(ApplicationException.class, () -> service.verify(null, PHONE, "000000", "1.2.3.4", "en"));
        assertEquals(null, user.getPhoneVerifiedAt());
        verify(users, never()).saveUser(any());
    }

    @Test
    void resendIsSilentForAConfirmedOrUnknownNumber() {
        user(true);
        service.resend(null, PHONE, "1.2.3.4", "en");
        service.resend(null, "01811111111", "1.2.3.4", "en");

        verify(codes, never()).issue(any(), any());
        verify(codes).limitRequests(Purpose.VERIFY_PHONE, "+8801811111111", "1.2.3.4", "en");
    }

    @Test
    void anSmsFailureDoesNotFailTheSignUp() {
        User user = user(false);
        doThrow(new IllegalStateException("gateway down")).when(sms).sendVerificationCode(any(), any());

        service.sendCode(user);
    }
}
