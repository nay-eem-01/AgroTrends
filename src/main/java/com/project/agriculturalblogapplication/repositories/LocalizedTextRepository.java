package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.LocalizedText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LocalizedTextRepository extends JpaRepository<LocalizedText, Long> {
    
    @Transactional
    void deleteAllById(long id);
}
