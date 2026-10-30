package com.project.agriculturalblogapplication.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LogbackMaskingPatternLayoutTest {

    private final LoggerContext context = new LoggerContext();
    private final LogbackMaskingPatternLayout layout = new LogbackMaskingPatternLayout();

    @BeforeEach
    void startLayout() {
        layout.setContext(context);
        layout.setPattern("%m");
        layout.start();
    }

    @Test
    void masksSecretJsonFields() {
        assertEquals("{\"email\":\"a@b.c\",\"password\":\"******\",\"refreshToken\":\"******\"}",
                format("{\"email\":\"a@b.c\",\"password\":\"hunter22\",\"refreshToken\":\"abc123\"}"));
    }

    @Test
    void masksKeyValueSecrets() {
        assertEquals("SignInRequest(identifier=017, password=******)",
                format("SignInRequest(identifier=017, password=hunter22)"));
        assertEquals("link /reset?token=****** sent", format("link /reset?token=abc123 sent"));
        assertEquals("Cookie: refresh_token=******; theme=dark", format("Cookie: refresh_token=abc123; theme=dark"));
    }

    @Test
    void masksBearerAndJwtShapedTokens() {
        assertEquals("Authorization: Bearer ******", format("Authorization: Bearer abc.def-ghi"));
        assertEquals("id token ****** rejected", format("id token eyJhbGciOi.eyJzdWIiOi.c2lnbmF0dXJl rejected"));
        assertFalse(format("google ya29.a0AfH6SMBxxxxxxxxxx").contains("ya29."));
    }

    @Test
    void leavesOrdinaryLinesAlone() {
        assertEquals("Creating new session for user 42", format("Creating new session for user 42"));
    }

    private String format(String message) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(context);
        event.setLevel(Level.INFO);
        event.setMessage(message);
        return layout.doLayout(event);
    }
}
