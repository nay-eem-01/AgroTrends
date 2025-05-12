package com.project.agriculturalblogapplication.Repositories;

import com.project.agriculturalblogapplication.Models.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
