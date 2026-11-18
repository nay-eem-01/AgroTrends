package com.project.agriculturalblogapplication.security;

import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.WebTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The refresh token rides in an HttpOnly cookie scoped to /api/auth (frontend request B12). */
class RefreshCookiesTest {

    @Test
    void aSignInSetsTheRefreshTokenAsAStrictHttpOnlyCookieAndKeepsItInTheBody() {
        ResponseEntity<HttpResponse> response = RefreshCookies.withCookie(HttpResponse.getResponseEntity(true, "ok",
                new WebTokenResponse("access", "refresh-123", "Bearer", null)));

        assertEquals("agrotrends_refresh=refresh-123; Path=/api/auth; Max-Age=864000; Expires="
                        + response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split("Expires=")[1].split(";")[0]
                        + "; Secure; HttpOnly; SameSite=Strict",
                response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        assertEquals("refresh-123", ((WebTokenResponse) response.getBody().getPayload()).getRefreshToken());
    }

    @Test
    void signOutClearsTheCookieAndOtherResponsesGetNone() {
        String cleared = RefreshCookies.cleared(HttpResponse.getResponseEntity(true, "bye"))
                .getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertEquals(true, cleared.startsWith("agrotrends_refresh=; Path=/api/auth; Max-Age=0;"));

        assertNull(RefreshCookies.withCookie(HttpResponse.getResponseEntity(true, "no tokens"))
                .getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }
}
