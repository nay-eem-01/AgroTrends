package com.project.agriculturalblogapplication.security;

import com.project.agriculturalblogapplication.security.jwt.JwtAuthenticationFilter;
import com.project.agriculturalblogapplication.security.jwt.JwtUtil;
import com.project.agriculturalblogapplication.security.entites.UserSession;
import com.project.agriculturalblogapplication.security.service.CustomUserDetailService;
import com.project.agriculturalblogapplication.security.service.CustomUserDetails;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final CustomUserDetailService userDetailsService = mock(CustomUserDetailService.class);
    private final UserSessionService sessions = mock(UserSessionService.class);
    private final JwtUtil jwtUtil = new JwtUtil("unit-test-secret-that-is-at-least-32-bytes-long");
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(userDetailsService, jwtUtil, sessions);
    private final FilterChain chain = mock(FilterChain.class);

    private String token;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        token = jwtUtil.generateAccessToken("farmer@example.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void givenUser(boolean mustChangePassword) {
        CustomUserDetails user = new CustomUserDetails(5L, "n", "farmer@example.com", "pw", Set.of(), List.of(), mustChangePassword);
        when(userDetailsService.loadUserByUsername("farmer@example.com")).thenReturn(user);
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    void validTokenWithActiveSessionAuthenticates() throws Exception {
        givenUser(false);
        when(sessions.getActiveSessionByToken(token)).thenReturn(new UserSession());

        filter.doFilter(request("/api/blogs/id/1"), new MockHttpServletResponse(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void signedOutTokenIsNotAuthenticatedEvenThoughSignatureIsValid() throws Exception {
        givenUser(false);
        when(sessions.getActiveSessionByToken(token)).thenReturn(null);

        filter.doFilter(request("/api/blogs/id/1"), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void garbageTokenDoesNotThrowAndStaysUnauthenticated() throws Exception {
        token = "not.a.jwt";

        filter.doFilter(request("/api/blogs/id/1"), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(sessions);
    }

    @Test
    void mustChangePasswordBlocksEverythingExceptChangePassword() throws Exception {
        givenUser(true);
        when(sessions.getActiveSessionByToken(token)).thenReturn(new UserSession());

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request("/api/blogs/create"), blocked, chain);
        assertEquals(403, blocked.getStatus());
        verifyNoInteractions(chain);

        SecurityContextHolder.clearContext();
        MockHttpServletResponse allowed = new MockHttpServletResponse();
        filter.doFilter(request("/api/user/change-password"), allowed, chain);
        assertEquals(200, allowed.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }
}
