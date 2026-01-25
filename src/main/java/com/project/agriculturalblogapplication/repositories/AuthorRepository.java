package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author,Long> {
    Optional<Author> findById(Long id);

    Optional<Author> findByUserId(Long userId);
}
