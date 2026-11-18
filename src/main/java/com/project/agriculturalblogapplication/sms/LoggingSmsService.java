package com.project.agriculturalblogapplication.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/** Development fallback used while no SMS provider is configured: codes are logged, not sent. */
@Slf4j
@Service
@ConditionalOnExpression("'${app.sms.provider:}'.isEmpty()")
public class LoggingSmsService implements SmsService {

    public LoggingSmsService() {
        log.warn("SMS_PROVIDER is not set: phone confirmation and reset codes will be written to the log instead of " +
                "sent. Configure an SMS provider before running in production.");
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        log.warn("[DEV SMS] phone confirmation code for {}: {}", to, code);
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        log.warn("[DEV SMS] password reset code for {}: {}", to, code);
    }
}
