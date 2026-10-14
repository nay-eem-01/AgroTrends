package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.ExpertApplication;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpertApplicationRepository extends JpaRepository<ExpertApplication, Long> {

    boolean existsByUserIdAndStatus(Long userId, ApplicationStatus status);

    Page<ExpertApplication> findAllByUserId(Long userId, Pageable pageable);
}
