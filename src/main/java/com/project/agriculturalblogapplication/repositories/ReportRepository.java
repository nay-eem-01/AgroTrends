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

import java.time.LocalDateTime;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTarget targetType, Long targetId);

    @EntityGraph(attributePaths = "reporter")
    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    /**
     * Closes every open report on one target as actioned, keeping what the content said (the snapshot taken when it
     * was hidden wins: the post may have been deleted since). Bulk updates skip the
     * auditing listener, so the moderator and time are set here.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Report r set r.status = com.project.agriculturalblogapplication.enums.ReportStatus.ACTIONED,
                   r.snapshot = coalesce(r.snapshot, :snapshot), r.lastModifiedBy = :by, r.lastModifiedDate = :at
            where r.targetType = :targetType and r.targetId = :targetId
              and r.status = com.project.agriculturalblogapplication.enums.ReportStatus.OPEN""")
    int closeAsActioned(@Param("targetType") ReportTarget targetType, @Param("targetId") Long targetId,
                        @Param("snapshot") String snapshot, @Param("by") String by, @Param("at") LocalDateTime at);

    long countByTargetTypeAndTargetIdAndStatus(ReportTarget targetType, Long targetId, ReportStatus status);

    /** Keeps what a post said on its open reports when it is hidden, so moderators can judge it. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Report r set r.snapshot = :snapshot
            where r.targetType = :targetType and r.targetId = :targetId
              and r.status = com.project.agriculturalblogapplication.enums.ReportStatus.OPEN""")
    int recordSnapshot(@Param("targetType") ReportTarget targetType, @Param("targetId") Long targetId,
                       @Param("snapshot") String snapshot);

    /**
     * Dismisses every open report on one target; 0 when none is open any more, e.g. another moderator removed the
     * post a moment earlier. Bulk updates skip auditing, so the moderator and time are set here.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Report r set r.status = com.project.agriculturalblogapplication.enums.ReportStatus.DISMISSED,
                   r.lastModifiedBy = :by, r.lastModifiedDate = :at
            where r.targetType = :targetType and r.targetId = :targetId
              and r.status = com.project.agriculturalblogapplication.enums.ReportStatus.OPEN""")
    int dismissAllOpen(@Param("targetType") ReportTarget targetType, @Param("targetId") Long targetId,
                       @Param("by") String by, @Param("at") LocalDateTime at);
}
