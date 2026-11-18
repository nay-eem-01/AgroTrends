package com.project.agriculturalblogapplication.security;

import com.project.agriculturalblogapplication.config.AiProperties;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Per-IP caps on endpoints that cost money or verify against Google, across accounts: the per-user AI quota alone
 * does not stop one machine with many accounts. Uses the client IP the servlet sees, so a reverse proxy must
 * forward it (roadmap 4.1).
 */
@Component
public class PaidEndpointRateLimitFilter extends OncePerRequestFilter {

    /** {@code endpoints} are "METHOD /path" patterns sharing one budget. */
    private record Rule(String name, List<String> endpoints, int max, Duration window) {}

    static final int GOOGLE_SIGN_INS_PER_HOUR = 30;

    private static final AntPathMatcher PATHS = new AntPathMatcher();

    private final AttemptLimiter attemptLimiter;
    private final List<Rule> rules;

    public PaidEndpointRateLimitFilter(AttemptLimiter attemptLimiter, AiProperties aiProperties) {
        this.attemptLimiter = attemptLimiter;
        this.rules = List.of(
                new Rule("ai", List.of("POST /api/ai/ask", "POST /api/ai/blog-assist",
                        "GET /api/blogs/id/*/related"), aiProperties.getDailyIpLimit(), Duration.ofDays(1)),
                new Rule("google", List.of("POST /api/auth/google"), GOOGLE_SIGN_INS_PER_HOUR, Duration.ofHours(1)));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        for (Rule rule : rules) {
            if (rule.endpoints().stream().anyMatch(pattern -> PATHS.match(pattern, endpoint))) {
                String key = "paid:" + rule.name() + ":ip:" + request.getRemoteAddr();
                if (attemptLimiter.isBlocked(key)) {
                    HttpResponse.writeError(response, HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ERROR_TOO_MANY_REQUESTS_FROM_NETWORK);
                    return;
                }
                attemptLimiter.record(key, rule.max(), rule.window());
                break;
            }
        }
        chain.doFilter(request, response);
    }
}
