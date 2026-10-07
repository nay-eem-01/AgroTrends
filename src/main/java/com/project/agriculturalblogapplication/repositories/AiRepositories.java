package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.AiAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AiRepositories extends JpaRepository<AiAnswer, Long> {
    Page<AiAnswer> findAllByUserId(Long userId, Pageable pageable);

    long countByUserIdAndCreationDateGreaterThanEqual(Long userId, LocalDateTime from);
}
