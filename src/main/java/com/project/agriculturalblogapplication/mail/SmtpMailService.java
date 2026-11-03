package com.project.agriculturalblogapplication.mail;

import com.project.agriculturalblogapplication.constatnt.AppConstants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("!'${spring.mail.host:}'.isEmpty()")
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;

    static final int DAILY_WARNING = 250;

    @Value("${app.mail.from}")
    private String from;

    private LocalDate countDay;

    private int sentToday;

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

    @Override
    public void send(Mail mail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(mail.to());
            helper.setSubject(mail.subject());
            helper.setText(mail.text() + "\n\n" + mail.actionLabel() + ": " + mail.actionUrl()
                    + "\n\n" + mail.unsubscribeLabel() + ": " + mail.unsubscribeUrl(), html(mail));
            // Gmail and Yahoo require one-click unsubscribe (RFC 8058) from bulk senders.
            message.setHeader("List-Unsubscribe", "<" + mail.unsubscribeUrl() + ">");
            message.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
            mailSender.send(message);
            countSent();
        } catch (MessagingException e) {
            throw new MailPreparationException(e);
        }
    }

    private static String html(Mail mail) {
        StringBuilder body = new StringBuilder("<div style=\"font-family:sans-serif;font-size:15px;line-height:1.5;max-width:560px\">");
        for (String paragraph : mail.text().split("\n\n")) {
            body.append("<p>").append(HtmlUtils.htmlEscape(paragraph).replace("\n", "<br>")).append("</p>");
        }
        return body.append("<p><a href=\"").append(HtmlUtils.htmlEscape(mail.actionUrl()))
                .append("\" style=\"display:inline-block;padding:10px 18px;background:#2f6b3a;color:#fff;border-radius:6px;text-decoration:none\">")
                .append(HtmlUtils.htmlEscape(mail.actionLabel())).append("</a></p>")
                .append("<p style=\"font-size:12px;color:#666\"><a href=\"").append(HtmlUtils.htmlEscape(mail.unsubscribeUrl()))
                .append("\" style=\"color:#666\">").append(HtmlUtils.htmlEscape(mail.unsubscribeLabel())).append("</a></p></div>")
                .toString();
    }

    /** Brevo's free plan sends 300 a day; warn before mail starts bouncing off the cap. */
    // shortcut: counts this instance's sends since midnight (Asia/Dhaka) in memory; read Brevo's own count if this matters.
    private synchronized void countSent() {
        LocalDate today = LocalDate.now(AppConstants.APP_ZONE);
        if (!today.equals(countDay)) {
            countDay = today;
            sentToday = 0;
        }
        if (++sentToday == DAILY_WARNING) {
            log.warn("{} notification e-mails sent today; Brevo's free plan stops at 300", DAILY_WARNING);
        }
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
