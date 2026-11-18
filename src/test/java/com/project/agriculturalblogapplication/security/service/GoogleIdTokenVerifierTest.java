package com.project.agriculturalblogapplication.security.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The checks on a Google ID token, with a local RSA key standing in for Google's published keys. */
class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "our-client.apps.googleusercontent.com";
    private static final String GOOGLE = "https://accounts.google.com";

    private final KeyPair googleKey = rsa();
    private final GoogleIdTokenVerifier verifier = verifier(CLIENT_ID, googleKey);

    @Test
    void acceptsAGoogleTokenIssuedForUs() throws Exception {
        GoogleIdentity identity = verifier.verify(token(googleKey, GOOGLE, CLIENT_ID, 300), "en");

        assertEquals("1234567890", identity.subject());
        assertEquals("someone@gmail.com", identity.email());
        assertTrue(identity.emailVerified());
        assertEquals("Someone", identity.name());
        // Google uses both issuer spellings.
        assertEquals("1234567890", verifier.verify(token(googleKey, "accounts.google.com", CLIENT_ID, 300), "en").subject());
    }

    @Test
    void refusesAnotherAudienceIssuerKeyOrAnExpiredTokenWith401() throws Exception {
        assertUnauthorized(token(googleKey, GOOGLE, "someone-elses-client", 300));
        assertUnauthorized(token(googleKey, "https://evil.example.com", CLIENT_ID, 300));
        assertUnauthorized(token(rsa(), GOOGLE, CLIENT_ID, 300));
        assertUnauthorized(token(googleKey, GOOGLE, CLIENT_ID, -300));
        assertUnauthorized("not-a-jwt");
    }

    @Test
    void answers503WhenNoClientIdIsConfigured() throws Exception {
        GoogleIdTokenVerifier disabled = verifier("", googleKey);

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> disabled.verify(token(googleKey, GOOGLE, CLIENT_ID, 300), "en"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getHttpStatus());
    }

    private void assertUnauthorized(String idToken) {
        ApplicationException e = assertThrows(ApplicationException.class, () -> verifier.verify(idToken, "en"));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getHttpStatus());
    }

    private static GoogleIdTokenVerifier verifier(String clientId, KeyPair key) {
        return new GoogleIdTokenVerifier(clientId, NimbusJwtDecoder.withPublicKey((RSAPublicKey) key.getPublic()).build());
    }

    private static String token(KeyPair key, String issuer, String audience, long expiresInSeconds) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer).audience(audience).subject("1234567890")
                .claim("email", "someone@gmail.com").claim("email_verified", true).claim("name", "Someone")
                .issueTime(Date.from(now.minusSeconds(600)))
                .expirationTime(Date.from(now.plusSeconds(expiresInSeconds)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(key.getPrivate()));
        return jwt.serialize();
    }

    private static KeyPair rsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
