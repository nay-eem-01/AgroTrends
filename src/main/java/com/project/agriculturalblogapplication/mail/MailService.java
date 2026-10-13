package com.project.agriculturalblogapplication.mail;

public interface MailService {

    void sendPasswordReset(String to, String resetLink);

    void sendEmailVerification(String to, String verifyLink);
}
