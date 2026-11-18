package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.DigestSent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DigestSentRepository extends JpaRepository<DigestSent, Long> {

    boolean existsByUserIdAndWeekStart(Long userId, LocalDate weekStart);
}
