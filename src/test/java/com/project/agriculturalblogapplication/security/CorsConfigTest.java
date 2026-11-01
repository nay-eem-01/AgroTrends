package com.project.agriculturalblogapplication.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Only the configured frontend origins may call the API from a browser. */
class CorsConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig(null, null, null);

    @Test
    void apiAllowsOnlyTheConfiguredOrigins() {
        CorsConfiguration configuration = securityConfig
                .corsConfigurationSource(List.of("https://agrotrends.app", "https://www.agrotrends.app"))
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/blogs/all"));

        assertEquals("https://www.agrotrends.app", configuration.checkOrigin("https://www.agrotrends.app"));
        assertNull(configuration.checkOrigin("http://localhost:5173"));
        assertNull(configuration.checkOrigin("https://evil.example"));
    }
}
