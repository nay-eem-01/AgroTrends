package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.RefreshTokenRepository;
import com.project.agriculturalblogapplication.constatnt.SecurityConstants;
import com.project.agriculturalblogapplication.entities.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(SecurityConstants.REFRESH_TOKEN_EXPIRATION_TIME));
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token, String lang) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.deleteByTokenValue(token.getToken());
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_REFRESH_TOKEN_EXPIRED, lang);
        }

        return token;
    }

    /**
     * Atomically consumes a refresh token (single use). Returns false if it was already used or never existed,
     * so two concurrent refreshes with the same token cannot both succeed.
     */
    public boolean consume(RefreshToken token) {
        return refreshTokenRepository.deleteByTokenValue(token.getToken()) > 0;
    }

    /** Revokes every refresh token of the user (sign-out, password change/reset, account deletion). */
    public void deleteAllByUserId(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    public Boolean deleteRefreshToken(RefreshToken refreshToken) {
        try{
            refreshTokenRepository.delete(refreshToken);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
