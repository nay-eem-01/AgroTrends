package com.project.agriculturalblogapplication.security.repository;

import com.project.agriculturalblogapplication.security.entites.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findFirstByUserIdOrderByCreatedDateDesc(Long userId);

    Optional<UserSession> findByTokenAndIsActive(String token, Boolean isActive);

    Optional<UserSession> findBySessionTokenAndIsSessionTokenValid(String token, Boolean isValid);

    List<UserSession> findAllByUserIdAndIsActive(Long userId, Boolean isActive);

    @Transactional
    void deleteAllByUserId(Long userId);
}
