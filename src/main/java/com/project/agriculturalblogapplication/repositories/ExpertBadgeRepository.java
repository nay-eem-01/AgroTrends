package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.ExpertBadge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExpertBadgeRepository extends JpaRepository<ExpertBadge, Long> {

    Optional<ExpertBadge> findByUserIdAndTopicId(Long userId, Long topicId);

    @EntityGraph(attributePaths = "topic")
    List<ExpertBadge> findAllByUserIdAndRevokedAtIsNull(Long userId);

    @Query("select b.user.id from ExpertBadge b where b.topic.id = :topicId and b.revokedAt is null")
    List<Long> activeExpertIds(@Param("topicId") Long topicId);

    /** Users with an active badge (in this topic, or any when null), not banned, most accepted answers first. */
    @Query(value = """
            SELECT b.user_id FROM expert_badges b JOIN users u ON u.id = b.user_id
            LEFT JOIN (SELECT a.user_id, count(*) AS accepted FROM questions q JOIN answers a ON a.id = q.accepted_answer_id
                       WHERE q.hidden = false AND a.hidden = false GROUP BY a.user_id) acc ON acc.user_id = b.user_id
            WHERE b.revoked_at IS NULL AND u.banned_at IS NULL AND (CAST(:topicId AS bigint) IS NULL OR b.category_id = :topicId)
            GROUP BY b.user_id, acc.accepted ORDER BY coalesce(acc.accepted, 0) DESC, b.user_id""",
            countQuery = """
            SELECT count(DISTINCT b.user_id) FROM expert_badges b JOIN users u ON u.id = b.user_id
            WHERE b.revoked_at IS NULL AND u.banned_at IS NULL AND (CAST(:topicId AS bigint) IS NULL OR b.category_id = :topicId)""",
            nativeQuery = true)
    Page<Long> expertIds(@Param("topicId") Long topicId, Pageable pageable);

    @Query("select count(distinct b.user.id) from ExpertBadge b where b.revokedAt is null and b.user.bannedAt is null")
    long countActiveExperts();

    /** Pairs of (userId, topicId) for the active badges of these users. */
    @Query("select b.user.id, b.topic.id from ExpertBadge b where b.user.id in :userIds and b.revokedAt is null")
    List<Object[]> activeTopics(@Param("userIds") Collection<Long> userIds);
}
