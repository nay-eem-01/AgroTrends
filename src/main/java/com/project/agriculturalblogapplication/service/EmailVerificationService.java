package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.EmailToken.Purpose;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

/** Proves a user owns their e-mail address with a single-use link valid for 24 hours. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailTokenService emailTokenService;
    private final UserService userService;
    private final MailService mailService;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    /**
     * Mails a confirmation link. A mail failure is logged, not thrown: the account already exists and the user can
     * ask for a new link.
     */
    public void sendLink(User user) {
        String token = emailTokenService.issue(user.getId(), Purpose.VERIFY_EMAIL);
        try {
            mailService.sendEmailVerification(user.getEmail(), frontendUrl + "/verify-email?token=" + token);
        } catch (RuntimeException e) {
            log.warn("Could not send the confirmation e-mail to user {}", user.getId(), e);
        }
    }

    /** Never reveals whether the e-mail has an account or is already confirmed. */
    public void resend(String rawEmail, String clientIp, String lang) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        emailTokenService.limitRequests(Purpose.VERIFY_EMAIL, email, clientIp, lang);

        User user = userService.findByEmail(email);
        if (user != null && user.getEmailVerifiedAt() == null) {
            sendLink(user);
        }
    }

    public void verify(String token, String lang) {
        User user = userService.findByIdWithException(emailTokenService.userIdFor(token, Purpose.VERIFY_EMAIL, lang), lang);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
            userService.saveUser(user);
        }
        emailTokenService.revoke(user.getId(), Purpose.VERIFY_EMAIL);
    }
}
