package com.project.agriculturalblogapplication.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import java.util.regex.Pattern;

/**
 * Server-side twin of the frontend's DOMPurify policy (agrotrends-web/src/lib/sanitize.ts): rich-text tags and
 * images stay; scripts, event handlers, styles, frames and forms go; links keep only http(s), mailto or relative
 * targets. Text with no tags is plain text and is stored as typed (escaping it would show "&amp;lt;" to readers).
 */
public final class SafeHtml {

    private static final Pattern HAS_TAG = Pattern.compile("</?[a-z][\\s\\S]*?>", Pattern.CASE_INSENSITIVE);

    private static final Safelist POLICY = Safelist.relaxed()
            .removeProtocols("a", "href", "ftp")
            .addProtocols("a", "href", "mailto")
            .preserveRelativeLinks(true);

    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings().prettyPrint(false);

    private SafeHtml() {}

    /** The readable text of HTML (or plain text), whitespace collapsed; "" for null. */
    public static String plainText(String content) {
        return content == null ? "" : Jsoup.parse(content).text();
    }

    public static String clean(String content) {
        if (content == null || !HAS_TAG.matcher(content).find()) {
            return content;
        }
        // The base URI only lets jsoup resolve and keep relative links; it never appears in the output.
        return Jsoup.clean(content, "https://agrotrends.invalid/", POLICY, OUTPUT);
    }
}
