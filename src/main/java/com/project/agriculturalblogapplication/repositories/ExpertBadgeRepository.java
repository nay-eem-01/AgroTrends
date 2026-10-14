package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.ExpertBadge;
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

    /** Pairs of (userId, topicId) for the active badges of these users. */
    @Query("select b.user.id, b.topic.id from ExpertBadge b where b.user.id in :userIds and b.revokedAt is null")
    List<Object[]> activeTopics(@Param("userIds") Collection<Long> userIds);
}
