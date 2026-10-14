package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.ExpertApplication;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpertApplicationRepository extends JpaRepository<ExpertApplication, Long> {

    boolean existsByUserIdAndStatus(Long userId, ApplicationStatus status);

    Page<ExpertApplication> findAllByUserId(Long userId, Pageable pageable);

    /** Decides a pending application; 0 when another admin decided it first. */
    @Modifying(flushAutomatically = true)
    @Query("update ExpertApplication a set a.status = :status, a.reviewNote = :note where a.id = :id"
            + " and a.status = com.project.agriculturalblogapplication.enums.ApplicationStatus.PENDING")
    int decide(@Param("id") Long id, @Param("status") ApplicationStatus status, @Param("note") String note);

    @EntityGraph(attributePaths = "user")
    Page<ExpertApplication> findAllByStatus(ApplicationStatus status, Pageable pageable);
}
