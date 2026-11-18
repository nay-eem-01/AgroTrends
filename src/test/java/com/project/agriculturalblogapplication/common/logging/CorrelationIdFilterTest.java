package com.project.agriculturalblogapplication.common.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void keepsASafeIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "abc-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seen.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals("abc-12345", seen.get());
        assertEquals("abc-12345", response.getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void replacesAnUnsafeIncomingId() throws Exception {
        // A client-supplied id is written into log files: no newlines or markup.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "evil\nINFO fake line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertNotNull(id);
        assertNotEquals("evil\nINFO fake line", id);
    }

    @Test
    void clearsTheIdAfterTheRequest() throws Exception {
        // Threads are pooled: a leftover id would tag the next request's lines.
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> { });

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
