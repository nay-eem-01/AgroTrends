package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.PhoneCode.Purpose;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.sms.SmsService;
import com.project.agriculturalblogapplication.util.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Confirms a mobile number with a 6-digit SMS code: sent at sign-up and when a number is added or changed. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final PhoneCodeService phoneCodeService;
    private final UserService userService;
    private final SmsService smsService;

    /** An SMS failure is logged, not thrown: the account already exists and the user can ask for a new code. */
    public void sendCode(User user) {
        String code = phoneCodeService.issue(user.getId(), Purpose.VERIFY_PHONE);
        try {
            smsService.sendVerificationCode(user.getMobileNumber(), code);
        } catch (RuntimeException e) {
            log.warn("Could not send the confirmation code to user {}", user.getId(), e);
        }
    }

    @EventListener
    public void onContactChanged(ContactChangedEvent event) {
        if (event.phoneChanged()) {
            sendCode(userService.findByIdWithException(event.userId(), null));
        }
    }

    /** Never reveals whether the number has an account or is already confirmed. */
    public void resend(String countryCode, String mobileNumber, String clientIp, String lang) {
        String phone = phone(countryCode, mobileNumber, lang);
        phoneCodeService.limitRequests(Purpose.VERIFY_PHONE, phone, clientIp, lang);

        User user = userService.findByMobileNumber(phone);
        if (user != null && user.getPhoneVerifiedAt() == null) {
            sendCode(user);
        }
    }

    /** 400 for a wrong or expired code and for a number with no account alike. */
    public void verify(String countryCode, String mobileNumber, String code, String clientIp, String lang) {
        String phone = phone(countryCode, mobileNumber, lang);
        phoneCodeService.limitChecks(clientIp, lang);

        User user = userService.findByMobileNumber(phone);
        phoneCodeService.consume(user == null ? null : user.getId(), Purpose.VERIFY_PHONE, code, lang);
        if (user.getPhoneVerifiedAt() == null) {
            user.setPhoneVerifiedAt(LocalDateTime.now());
            userService.saveUser(user);
        }
    }

    private static String phone(String countryCode, String mobileNumber, String lang) {
        String phone = PhoneNumbers.normalise(countryCode, mobileNumber);
        if (phone == null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_MOBILE_NUMBER, lang);
        }
        return phone;
    }
}
