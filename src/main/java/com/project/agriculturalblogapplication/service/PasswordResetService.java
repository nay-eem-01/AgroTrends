package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.EmailToken.Purpose;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * E-mail based password reset: a single-use link valid for 15 minutes (`EmailTokenService`). Requesting a reset
 * never reveals whether the e-mail has an account.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final EmailTokenService emailTokenService;
    private final UserService userService;
    private final MailService mailService;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    public void requestReset(String rawEmail, String clientIp, String lang) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        emailTokenService.limitRequests(Purpose.RESET_PASSWORD, email, clientIp, lang);

        User user = userService.findByEmail(email);
        if (user == null) {
            return;
        }
        String token = emailTokenService.issue(user.getId(), Purpose.RESET_PASSWORD);
        mailService.sendPasswordReset(user.getEmail(), frontendUrl + "/reset-password?token=" + token);
    }

    public void reset(String token, String newPassword, String lang) {
        User user = userService.findByIdWithException(emailTokenService.userIdFor(token, Purpose.RESET_PASSWORD, lang), lang);
        // The e-mailed link proves the user owns the address (saved together with the new password).
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
        }
        // Validates the password policy first, so a weak password does not burn the token.
        userService.setNewPassword(user, newPassword, lang);
        emailTokenService.revoke(user.getId(), Purpose.RESET_PASSWORD);
    }
}
