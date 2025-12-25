package com.project.agriculturalblogapplication.constatnt;

import java.util.concurrent.TimeUnit;

public class SecurityConstants {
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long EXPIRATION_TIME = TimeUnit.HOURS.toMillis(1);
    public static final long REFRESH_TOKEN_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(10);
    public static final long SESSION_TOKEN_EXPIRATION_TIME = TimeUnit.DAYS.toSeconds(21);
    public static final String SECRET = "2034f6e32958647fdff75d265b455ebf2034f6e32958647fdff75d265b455ebf2034f6e32958647fdff75d265b455ebf";
    public static final String[] JWTDisabledAntMatchers = {
            "/swagger-ui.html",
            "/api/public",
            "/api/auth/**",
            "/api/admin/sign-in",
            "/swagger-ui/**",
            "/api-docs/**",
            "/api/verify/**",
            "/v3/api-docs/**",
            "/configuration/ui",
            "/swagger-resources/**",
            "/configuration/security",
            "/webjars/**",
            "/api/auth/refreshtoken",

    };

    private SecurityConstants() {
    }
}
