package com.project.agriculturalblogapplication.security;

import com.project.agriculturalblogapplication.config.AiProperties;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PaidEndpointRateLimitFilterTest {

    private final AiProperties ai = new AiProperties();
    private final PaidEndpointRateLimitFilter filter;

    PaidEndpointRateLimitFilterTest() {
        ai.setDailyIpLimit(2);
        filter = new PaidEndpointRateLimitFilter(new AttemptLimiter(), ai);
    }

    @Test
    void aiCallsShareOneDailyBudgetPerIp() throws Exception {
        assertEquals(200, status("POST", "/api/ai/ask", "1.1.1.1"));
        assertEquals(200, status("GET", "/api/blogs/id/9/related", "1.1.1.1"));

        assertEquals(429, status("POST", "/api/questions/id/4/ai-draft", "1.1.1.1"));
        assertEquals(200, status("POST", "/api/ai/ask", "2.2.2.2"));
    }

    @Test
    void otherEndpointsAreNotCounted() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, status("POST", "/api/questions/create", "1.1.1.1"));
            assertEquals(200, status("GET", "/api/ai/history", "1.1.1.1"));
        }
    }

    @Test
    void googleSignInHasItsOwnHourlyCap() throws Exception {
        for (int i = 0; i < PaidEndpointRateLimitFilter.GOOGLE_SIGN_INS_PER_HOUR; i++) {
            assertEquals(200, status("POST", "/api/auth/google", "1.1.1.1"));
        }
        assertEquals(429, status("POST", "/api/auth/google", "1.1.1.1"));
        assertEquals(200, status("POST", "/api/ai/ask", "1.1.1.1"));
    }

    private int status(String method, String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));
        return response.getStatus();
    }
}
