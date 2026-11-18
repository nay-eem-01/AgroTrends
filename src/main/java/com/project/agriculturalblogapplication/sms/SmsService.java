package com.project.agriculturalblogapplication.sms;

/** Sends text messages. {@code to} is an E.164 number. */
public interface SmsService {

    void sendVerificationCode(String to, String code);

    void sendPasswordResetCode(String to, String code);
}
