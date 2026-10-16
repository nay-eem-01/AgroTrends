package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.PhoneCode.Purpose;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.sms.SmsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhonePasswordResetServiceTest {

    private static final String PHONE = "+8801712345678";

    private final PhoneCodeService codes = mock(PhoneCodeService.class);
    private final UserService users = mock(UserService.class);
    private final SmsService sms = mock(SmsService.class);
    private final PhonePasswordResetService service = new PhonePasswordResetService(codes, users, sms);

    private User user() {
        User user = new User();
        user.setId(4L);
        user.setMobileNumber(PHONE);
        when(users.findByMobileNumber(PHONE)).thenReturn(user);
        return user;
    }

    @Test
    void aKnownNumberGetsAResetCode() {
        user();
        when(codes.issue(4L, Purpose.RESET_PASSWORD)).thenReturn("654321");

        service.requestReset("+880", "01712345678", "1.2.3.4", "en");

        verify(codes).limitRequests(Purpose.RESET_PASSWORD, PHONE, "1.2.3.4", "en");
        verify(sms).sendPasswordResetCode(PHONE, "654321");
    }

    @Test
    void anUnknownNumberIsSilent() {
        service.requestReset(null, "01811111111", "1.2.3.4", "en");

        verify(codes).limitRequests(Purpose.RESET_PASSWORD, "+8801811111111", "1.2.3.4", "en");
        verify(sms, never()).sendPasswordResetCode(any(), any());
    }

    @Test
    void theRightCodeSetsThePasswordAndConfirmsTheNumber() {
        User user = user();

        service.reset(null, PHONE, "654321", "NewPassw0rd!", "1.2.3.4", "en");

        verify(codes).consume(4L, Purpose.RESET_PASSWORD, "654321", "en");
        verify(users).setNewPassword(user, "NewPassw0rd!", "en");
        assertNotNull(user.getPhoneVerifiedAt());
    }

    @Test
    void aWeakPasswordIsRefusedWithoutBurningTheCode() {
        user();

        assertThrows(ApplicationException.class, () -> service.reset(null, PHONE, "654321", "weak", "1.2.3.4", "en"));

        verify(codes, never()).consume(any(), any(), any(), any());
        verify(users, never()).setNewPassword(any(), any(), any());
    }
}
