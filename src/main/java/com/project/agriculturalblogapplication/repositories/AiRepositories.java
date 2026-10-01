package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.AiAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiRepositories extends JpaRepository<AiAnswer, Long> {
    List<AiAnswer> findAllByUserId(Long userId);
}
