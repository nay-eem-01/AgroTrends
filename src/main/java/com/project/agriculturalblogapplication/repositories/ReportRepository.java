package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Report;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTarget targetType, Long targetId);

    @EntityGraph(attributePaths = "reporter")
    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    /** Closes every open report on one target as actioned, keeping what the content said. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Report r set r.status = com.project.agriculturalblogapplication.enums.ReportStatus.ACTIONED,
                   r.snapshot = :snapshot
            where r.targetType = :targetType and r.targetId = :targetId
              and r.status = com.project.agriculturalblogapplication.enums.ReportStatus.OPEN""")
    int closeAsActioned(@Param("targetType") ReportTarget targetType, @Param("targetId") Long targetId,
                        @Param("snapshot") String snapshot);
}
