package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findAllByUser(User user);
}
