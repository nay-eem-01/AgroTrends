package com.project.agriculturalblogapplication.security;

import com.project.agriculturalblogapplication.constatnt.SecurityConstants;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.WebTokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

/**
 * The refresh token in an HttpOnly cookie, so script on the page (an XSS) cannot read it. Sent only to /api/auth.
 * The token stays in the response body too for one release, so the deployed web keeps working; then drop it there.
 */
public final class RefreshCookies {

    public static final String NAME = "agrotrends_refresh";

    private static final String PATH = "/api/auth";

    private RefreshCookies() {
    }

    /** The sign-in or refresh response, with the new refresh token also set as the cookie. */
    public static ResponseEntity<HttpResponse> withCookie(ResponseEntity<HttpResponse> response) {
        if (!(response.getBody() != null && response.getBody().getPayload() instanceof WebTokenResponse tokens)) {
            return response;
        }
        return withHeader(response, cookie(tokens.getRefreshToken(),
                Duration.ofMillis(SecurityConstants.REFRESH_TOKEN_EXPIRATION_TIME)));
    }

    /** Sign-out: the browser drops the cookie. */
    public static ResponseEntity<HttpResponse> cleared(ResponseEntity<HttpResponse> response) {
        return withHeader(response, cookie("", Duration.ZERO));
    }

    private static ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(true).sameSite("Strict").path(PATH).maxAge(maxAge).build();
    }

    private static ResponseEntity<HttpResponse> withHeader(ResponseEntity<HttpResponse> response, ResponseCookie cookie) {
        return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders())
                .header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response.getBody());
    }
}
