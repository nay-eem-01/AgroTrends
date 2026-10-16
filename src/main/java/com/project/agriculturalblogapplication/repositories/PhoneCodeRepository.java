package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.PhoneCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PhoneCodeRepository extends JpaRepository<PhoneCode, Long> {

    Optional<PhoneCode> findByUserIdAndPurpose(Long userId, PhoneCode.Purpose purpose);

    @Modifying
    @Transactional
    @Query("delete from PhoneCode c where c.userId = :userId and c.purpose = :purpose")
    int deleteAllByUserIdAndPurpose(@Param("userId") Long userId, @Param("purpose") PhoneCode.Purpose purpose);
}
