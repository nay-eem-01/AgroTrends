package com.project.agriculturalblogapplication.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeHtmlTest {

    @Test
    void editorFormattingSurvivesUnchanged() {
        String html = "<h2>Rice blast</h2><p>Spray <strong>early</strong>, see <a href=\"https://bari.gov.bd\">BARI</a>.</p>"
                + "<ul><li>Dose</li></ul><img src=\"/uploads/x.png\" alt=\"leaf\">";

        assertEquals(html, SafeHtml.clean(html));
    }

    @Test
    void scriptsHandlersStylesAndFramesAreRemoved() {
        String dirty = "<p onclick=\"steal()\" style=\"color:red\">Hi</p><script>alert(1)</script>"
                + "<iframe src=\"https://evil\"></iframe><img src=\"x\" onerror=\"alert(1)\">";

        String clean = SafeHtml.clean(dirty);

        assertEquals("<p>Hi</p><img src=\"x\">", clean);
    }

    @Test
    void javascriptLinksLoseTheirTarget() {
        String clean = SafeHtml.clean("<a href=\"javascript:alert(1)\">x</a><a href=\"mailto:a@b.com\">m</a>");

        assertFalse(clean.contains("javascript"));
        assertTrue(clean.contains("href=\"mailto:a@b.com\""));
    }

    @Test
    void plainTextIsStoredAsTyped() {
        assertEquals("Use 2 < 3 kg & water", SafeHtml.clean("Use 2 < 3 kg & water"));
        assertNull(SafeHtml.clean(null));
    }
}
