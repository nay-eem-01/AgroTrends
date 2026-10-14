package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.ReputationEvent;
import com.project.agriculturalblogapplication.model.response.TopicReputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReputationEventRepository extends JpaRepository<ReputationEvent, Long> {

    @Query("select coalesce(sum(e.points), 0) from ReputationEvent e where e.user.id = :userId")
    long totalFor(@Param("userId") Long userId);

    /** [userId, total] for each of {@code userIds} that has any events. */
    @Query("select e.user.id, sum(e.points) from ReputationEvent e where e.user.id in :userIds group by e.user.id")
    List<Object[]> totalsFor(@Param("userIds") Collection<Long> userIds);

    @Query("""
            select new com.project.agriculturalblogapplication.model.response.TopicReputation(c.id, c.categoryName, sum(e.points))
            from ReputationEvent e left join e.topic c
            where e.user.id = :userId
            group by c.id, c.categoryName
            order by sum(e.points) desc
            """)
    List<TopicReputation> byTopic(@Param("userId") Long userId);
}
