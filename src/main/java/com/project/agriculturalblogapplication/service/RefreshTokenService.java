package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.RefreshTokenRepository;
import com.project.agriculturalblogapplication.constatnt.SecurityConstants;
import com.project.agriculturalblogapplication.entities.RefreshToken;
import lombok.RequiredArgsConstructor;
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

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new ApplicationException(token.getToken() + " Refresh token was expired. Please make strigaCards new signin request");
        }

        return token;
    }

    public Boolean deleteByCredentialId(Long userId) {
        List<RefreshToken> refreshTokens = this.refreshTokenRepository.findByUserIdAndExpiryDateIsBefore(userId,Instant.now());
        this.refreshTokenRepository.deleteAll(refreshTokens);
        return true;
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
