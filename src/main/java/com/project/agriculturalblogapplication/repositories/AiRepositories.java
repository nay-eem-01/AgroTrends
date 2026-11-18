package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.AiAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.project.agriculturalblogapplication.entities.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AiRepositories extends JpaRepository<AiAnswer, Long> {
    Page<AiAnswer> findAllByUserId(Long userId, Pageable pageable);

    long countByUserIdAndCreationDateGreaterThanEqual(Long userId, LocalDateTime from);

    Optional<AiAnswer> findByIdAndUserId(Long id, Long userId);

    /** Links the ask to its public question only if it has none yet: a double submit cannot publish twice. */
    @Modifying(flushAutomatically = true)
    @Query("update AiAnswer a set a.publishedQuestion = :question where a.id = :id and a.publishedQuestion is null")
    int markPublished(@Param("id") Long id, @Param("question") Question question);
}
