package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Report;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTarget targetType, Long targetId);
}
