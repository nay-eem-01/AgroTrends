package com.project.agriculturalblogapplication.security.jwt;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.constatnt.SecurityConstants;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.security.service.CustomUserDetailService;
import com.project.agriculturalblogapplication.security.service.CustomUserDetails;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** The only endpoints reachable while an account is flagged mustChangePassword. */
    private static final Set<String> ALLOWED_WHILE_PASSWORD_CHANGE_PENDING = Set.of(
            "/api/user/change-password",
            "/api/user/me",
            "/api/auth/sign-out");

    /** Account housekeeping an unconfirmed user may still do (besides /api/auth/**); every other write needs it. */
    private static final Set<String> WRITES_ALLOWED_BEFORE_EMAIL_CONFIRMED = Set.of(
            "/api/user/update",
            "/api/user/change-password",
            "/api/user/delete");

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final CustomUserDetailService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserSessionService userSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(SecurityConstants.HEADER_STRING);

        if (authHeader != null && authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String jwt = authHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
            CustomUserDetails userDetails = authenticate(jwt);

            if (userDetails != null) {
                String refusal = refusal(userDetails, request);
                if (refusal != null) {
                    HttpResponse.writeError(response, HttpStatus.FORBIDDEN, refusal);
                    return;
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Why a signed-in request is refused before it reaches a controller, or null when it may go on. */
    static String refusal(CustomUserDetails user, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (user.isBanned()) {
            return ErrorCode.ERROR_ACCOUNT_BANNED;
        }
        if (user.isMustChangePassword() && !ALLOWED_WHILE_PASSWORD_CHANGE_PENDING.contains(uri)) {
            return "Password change required before using this API.";
        }
        if (!user.isEmailVerified() && !READ_METHODS.contains(request.getMethod())
                && !uri.startsWith("/api/auth/") && !WRITES_ALLOWED_BEFORE_EMAIL_CONFIRMED.contains(uri)) {
            return ErrorCode.ERROR_EMAIL_NOT_VERIFIED;
        }
        return null;
    }

    /**
     * Returns the user for a valid access token that still has an active session, or null. A malformed, expired,
     * forged or signed-out token never throws: the request simply stays unauthenticated and the entry point
     * answers 401.
     */
    private CustomUserDetails authenticate(String jwt) {
        try {
            String username = jwtUtil.extractUsername(jwt);
            if (username == null) {
                return null;
            }

            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);
            if (!jwtUtil.isAccessTokenValid(jwt, userDetails)) {
                return null;
            }

            // Sign-out and password changes deactivate the session row; the signature alone is not enough.
            if (userSessionService.getActiveSessionByToken(jwt) == null) {
                return null;
            }
            return userDetails;
        } catch (RuntimeException e) {
            log.debug("Rejected bearer token: {}", e.getClass().getSimpleName());
            return null;
        }
    }
}
