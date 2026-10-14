package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.ReputationEvent;
import com.project.agriculturalblogapplication.model.TopicTotals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReputationEventRepository extends JpaRepository<ReputationEvent, Long> {

    @Query("select coalesce(sum(e.points), 0) from ReputationEvent e where e.user.id = :userId")
    long totalFor(@Param("userId") Long userId);

    /** Per user and topic: points and net accepted answers (a reversal row cancels its accept). */
    @Query("""
            select new com.project.agriculturalblogapplication.model.TopicTotals(e.user.id, c.id, c.categoryName,
                       sum(e.points),
                       sum(case when e.reason = com.project.agriculturalblogapplication.enums.ReputationReason.ANSWER_ACCEPTED
                                then sign(e.points) else 0 end))
            from ReputationEvent e left join e.topic c
            where e.user.id in :userIds
            group by e.user.id, c.id, c.categoryName
            """)
    List<TopicTotals> topicTotals(@Param("userIds") Collection<Long> userIds);
}
