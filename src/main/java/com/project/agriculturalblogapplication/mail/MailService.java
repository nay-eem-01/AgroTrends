package com.project.agriculturalblogapplication.mail;

public interface MailService {

    void sendPasswordReset(String to, String resetLink);

    void sendEmailVerification(String to, String verifyLink);

    /** Answer and digest e-mails; callers catch and log failures, they never fail the action. */
    void send(Mail mail);
}
