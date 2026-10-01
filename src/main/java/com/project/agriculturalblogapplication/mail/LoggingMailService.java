package com.project.agriculturalblogapplication.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/** Development fallback used when no SMTP host is configured: the reset link is logged, not sent. */
@Slf4j
@Service
@ConditionalOnExpression("'${spring.mail.host:}'.isEmpty()")
public class LoggingMailService implements MailService {

    public LoggingMailService() {
        log.warn("MAIL_HOST is not set: password-reset links will be written to the log instead of e-mailed. " +
                "Configure SMTP before running in production.");
    }

    @Override
    public void sendPasswordReset(String to, String resetLink) {
        log.warn("[DEV MAIL] password reset for {}: {}", to, resetLink);
    }
}
