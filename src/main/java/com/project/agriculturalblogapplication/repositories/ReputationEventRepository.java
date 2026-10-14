package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.ReputationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReputationEventRepository extends JpaRepository<ReputationEvent, Long> {

    @Query("select coalesce(sum(e.points), 0) from ReputationEvent e where e.user.id = :userId")
    long totalFor(@Param("userId") Long userId);
}
