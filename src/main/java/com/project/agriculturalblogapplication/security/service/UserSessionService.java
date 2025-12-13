package com.project.agriculturalblogapplication.security.service;

import com.project.agriculturalblogapplication.security.entites.UserSession;
import com.project.agriculturalblogapplication.security.model.request.CreateUserSessionRequest;
import com.project.agriculturalblogapplication.security.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    public List<UserSession> findActiveUserSession(Long userId) {
        return userSessionRepository.findAllByUserIdAndIsActive(userId, true);
    }

    public UserSession getActiveSessionByUserId(Long userId) {
        return userSessionRepository.findFirstByUserIdOrderByCreatedDateDesc(userId).orElse(null);
    }

    public UserSession getActiveSessionByToken(String token) {
        return userSessionRepository.findByTokenAndIsActive(token, true).orElse(null);
    }

    public UserSession findByValidSessionToken(String sessionToken) {
        return userSessionRepository.findBySessionTokenAndIsSessionTokenValid(sessionToken, true).orElse(null);
    }

    public UserSession createNewSession(CreateUserSessionRequest request) {
        log.info("Creating new session");
        UserSession userSession = new UserSession();
        userSession.setToken(request.getToken());
        userSession.setTokenType(request.getTokenType());
        userSession.setIssuesAt(OffsetDateTime.now(ZoneOffset.UTC));
        userSession.setExpiresAt(userSession.getIssuesAt().plusDays(3));
        userSession.setSessionToken(UUID.randomUUID().toString());
        userSession.setIsSessionTokenValid(true);
        userSession.setPlatformType(request.getPlatformType());
        userSession.setIsActive(true);
        userSession.setUserDeviceId(request.getUserDeviceId());
        userSession.setUserType(request.getUserType());
        userSession.setUserId(request.getUserId());
        return userSessionRepository.save(userSession);
    }

    public void deactivateSession(UserSession userSession) {
        if (userSession == null) return;

        userSession.setDeactivatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        userSession.setIsActive(false);
        userSessionRepository.save(userSession);
    }

    public void deactivatePreviousSession(Long userId) {
        List<UserSession> previousSessions = findActiveUserSession(userId);
        for (UserSession userSession : previousSessions) {
            userSession.setDeactivatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            userSession.setIsActive(false);
            userSessionRepository.save(userSession);
        }
    }

    public void deleteAllByUserId(Long userId) {
        userSessionRepository.deleteAllByUserId(userId);
    }
}
