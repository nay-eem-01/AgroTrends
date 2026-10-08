package com.project.agriculturalblogapplication.constatnt;

import java.util.concurrent.TimeUnit;

public class SecurityConstants {
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(15);
    public static final long REFRESH_TOKEN_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(10);
    public static final long SESSION_TOKEN_EXPIRATION_TIME = TimeUnit.DAYS.toSeconds(21);
    public static final String[] JWTDisabledAntMatchers = {
            "/swagger-ui.html",
            "/api/public",
            "/api/auth/**",
            "/api/admin/sign-in",
            "/uploads/**",
            "/swagger-ui/**",
            "/api-docs/**",
            "/api/verify/**",
            "/v3/api-docs/**",
            "/configuration/ui",
            "/swagger-resources/**",
            "/configuration/security",
            "/webjars/**",

    };

    /**
     * Reading is open to everyone; posting is not. Only GET on these paths is public - the same paths with any
     * other method (and every path not listed: drafts, bookmarks, the following feed, claps, related posts, AI)
     * still need a token. Related posts stay signed-in because each call embeds text with a paid model.
     */
    public static final String[] PUBLIC_GET_MATCHERS = {
            "/api/blogs/all",
            "/api/blogs/all/**",
            "/api/blogs/search",
            "/api/blogs/id/*",
            "/api/blogs/slug/*",
            "/api/feed/latest",
            "/api/feed/trending",
            "/api/categories/all",
            "/api/tags",
            "/api/authors/*",
            "/api/comments/blog/*",
            "/api/comments/replies/*",
            "/api/comments/id/*",
            "/api/questions/all",
            "/api/questions/all/**",
            "/api/questions/id/*",
            "/api/answers/question/*",
            "/api/answers/replies/*",
            "/api/answers/id/*",
    };

    private SecurityConstants() {
    }
}
