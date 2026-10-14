package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface QuestionAiAnswerRepository extends JpaRepository<QuestionAiAnswer, Long> {

    Optional<QuestionAiAnswer> findByQuestionId(Long questionId);

    long countByQuestionUserIdAndCreationDateGreaterThanEqual(Long userId, LocalDateTime since);
}
