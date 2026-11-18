package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.EmailToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByTokenHashAndPurpose(String tokenHash, EmailToken.Purpose purpose);

    @Modifying
    @Transactional
    @Query("delete from EmailToken t where t.userId = :userId and t.purpose = :purpose")
    int deleteAllByUserIdAndPurpose(@Param("userId") Long userId, @Param("purpose") EmailToken.Purpose purpose);
}
