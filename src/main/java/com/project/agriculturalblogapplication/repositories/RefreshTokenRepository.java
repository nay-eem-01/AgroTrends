package com.project.agriculturalblogapplication.repositories;


import com.project.agriculturalblogapplication.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findById(long id);

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserIdAndExpiryDateIsBefore(Long userId, Instant currentTime);

    @Modifying
    @Transactional
    @Query("delete from RefreshToken r where r.token = :token")
    int deleteByTokenValue(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("delete from RefreshToken r where r.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
