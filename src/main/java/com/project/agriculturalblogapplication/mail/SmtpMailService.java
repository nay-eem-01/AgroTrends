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
        send(to, "Reset your AgroTrends password", "We received a request to reset your password.\n\n"
                + "Open this link within 15 minutes to choose a new one:\n" + resetLink + "\n\n"
                + "If you did not ask for this, you can ignore this e-mail; your password has not changed.");
    }

    @Override
    public void sendEmailVerification(String to, String verifyLink) {
        send(to, "Confirm your AgroTrends e-mail", "Welcome to AgroTrends!\n\n"
                + "Open this link within 24 hours to confirm your e-mail address:\n" + verifyLink + "\n\n"
                + "You need a confirmed address to ask, answer and publish. "
                + "If you did not create an account, you can ignore this e-mail.");
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
