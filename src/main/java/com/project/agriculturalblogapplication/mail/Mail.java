package com.project.agriculturalblogapplication.mail;

/**
 * A notification e-mail: plain text, an action link (shown as a button in the HTML part), and the one-click
 * unsubscribe URL that also goes into the List-Unsubscribe headers.
 */
public record Mail(String to, String subject, String text, String actionLabel, String actionUrl,
                   String unsubscribeLabel, String unsubscribeUrl) {
}
