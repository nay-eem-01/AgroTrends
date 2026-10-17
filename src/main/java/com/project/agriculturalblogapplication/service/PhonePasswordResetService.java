package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.PhoneCode.Purpose;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.sms.SmsService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.util.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Password reset by SMS: a 6-digit code valid for 10 minutes (`PhoneCodeService`). Requesting a code never reveals
 * whether the number has an account.
 */
@Service
@RequiredArgsConstructor
public class PhonePasswordResetService {

    private final PhoneCodeService phoneCodeService;
    private final UserService userService;
    private final SmsService smsService;

    public void requestReset(String countryCode, String mobileNumber, String clientIp, String lang) {
        String phone = phone(countryCode, mobileNumber, lang);
        phoneCodeService.limitRequests(Purpose.RESET_PASSWORD, phone, clientIp, lang);

        User user = userService.findByMobileNumber(phone);
        if (user == null) {
            return;
        }
        smsService.sendPasswordResetCode(phone, phoneCodeService.issue(user.getId(), Purpose.RESET_PASSWORD));
    }

    public void reset(String countryCode, String mobileNumber, String code, String newPassword, String clientIp,
                      String lang) {
        String phone = phone(countryCode, mobileNumber, lang);
        phoneCodeService.limitChecks(clientIp, lang);
        // The policy is checked first, so a weak password does not burn the code.
        String invalidPasswordMessage = CommonUtils.getInvalidPasswordMessage(newPassword);
        if (invalidPasswordMessage != null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, invalidPasswordMessage, lang);
        }

        User user = userService.findByMobileNumber(phone);
        phoneCodeService.consume(user == null ? null : user.getId(), Purpose.RESET_PASSWORD, code, lang);
        // The code proves the user owns the number (saved together with the new password).
        if (user.getPhoneVerifiedAt() == null) {
            user.setPhoneVerifiedAt(LocalDateTime.now());
        }
        userService.setNewPassword(user, newPassword, lang);
    }

    private static String phone(String countryCode, String mobileNumber, String lang) {
        String phone = PhoneNumbers.normalise(countryCode, mobileNumber);
        if (phone == null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_MOBILE_NUMBER, lang);
        }
        return phone;
    }
}
