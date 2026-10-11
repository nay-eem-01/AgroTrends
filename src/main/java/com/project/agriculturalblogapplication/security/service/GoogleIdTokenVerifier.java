package com.project.agriculturalblogapplication.security.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Checks a Google ID token (Google Identity Services, ID-token flow) locally: Google's signature (keys from its
 * JWKS, cached by Nimbus), {@code iss} Google, {@code aud} our client id, not expired. No call to Google per
 * sign-in beyond refreshing the keys.
 */
@Component
public class GoogleIdTokenVerifier {

    static final String GOOGLE_KEYS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;

    private final JwtDecoder decoder;

    @Autowired
    public GoogleIdTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this(clientId, NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS).build());
    }

    /** For tests: a decoder with its own signing key, carrying the same Google rules. */
    GoogleIdTokenVerifier(String clientId, NimbusJwtDecoder decoder) {
        this.clientId = clientId;
        decoder.setJwtValidator(googleRules(clientId));
        this.decoder = decoder;
    }

    /**
     * @throws ApplicationException 503 when no client id is configured, 401 for anything that is not a valid,
     *                              unexpired Google token issued for this app
     */
    public GoogleIdentity verify(String idToken, String lang) {
        if (!StringUtils.hasText(clientId)) {
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.ERROR_GOOGLE_SIGN_IN_DISABLED, lang);
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_INVALID_GOOGLE_TOKEN, lang);
        }
        // Google sends email_verified as a boolean; some older tokens carried the string "true".
        Object verified = jwt.getClaims().get("email_verified");
        return new GoogleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(verified) || "true".equals(verified), jwt.getClaimAsString("name"));
    }

    private static OAuth2TokenValidator<Jwt> googleRules(String clientId) {
        OAuth2TokenValidator<Jwt> issuer = jwt -> GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Not issued by Google", null));
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Not for this app", null));
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuer, audience);
    }
}
