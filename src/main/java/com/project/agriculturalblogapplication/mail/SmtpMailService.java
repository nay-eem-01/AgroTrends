package com.project.agriculturalblogapplication.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("!'${spring.mail.host:}'.isEmpty()")
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendPasswordReset(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Reset your AgroTrends password");
        message.setText("We received a request to reset your password.\n\n"
                + "Open this link within 15 minutes to choose a new one:\n" + resetLink + "\n\n"
                + "If you did not ask for this, you can ignore this e-mail; your password has not changed.");
        mailSender.send(message);
    }
}
