package com.project.agriculturalblogapplication.mail;

import org.springframework.web.util.HtmlUtils;

/**
 * A notification e-mail: plain text, an action link (shown as a button in the HTML part), and the one-click
 * unsubscribe URL that also goes into the List-Unsubscribe headers.
 */
public record Mail(String to, String subject, String text, String actionLabel, String actionUrl,
                   String unsubscribeLabel, String unsubscribeUrl) {

    /** The plain-text part: the text, then the links written out. */
    public String plainText() {
        return text + "\n\n" + actionLabel + ": " + actionUrl + "\n\n" + unsubscribeLabel + ": " + unsubscribeUrl;
    }

    /** The HTML part: every value escaped, paragraphs from blank lines, the action as a button. */
    public String html() {
        StringBuilder body = new StringBuilder("<div style=\"font-family:sans-serif;font-size:15px;line-height:1.5;max-width:560px\">");
        for (String paragraph : text.split("\n\n")) {
            body.append("<p>").append(HtmlUtils.htmlEscape(paragraph).replace("\n", "<br>")).append("</p>");
        }
        return body.append("<p><a href=\"").append(HtmlUtils.htmlEscape(actionUrl))
                .append("\" style=\"display:inline-block;padding:10px 18px;background:#2f6b3a;color:#fff;border-radius:6px;text-decoration:none\">")
                .append(HtmlUtils.htmlEscape(actionLabel)).append("</a></p>")
                .append("<p style=\"font-size:12px;color:#666\"><a href=\"").append(HtmlUtils.htmlEscape(unsubscribeUrl))
                .append("\" style=\"color:#666\">").append(HtmlUtils.htmlEscape(unsubscribeLabel)).append("</a></p></div>")
                .toString();
    }
}
