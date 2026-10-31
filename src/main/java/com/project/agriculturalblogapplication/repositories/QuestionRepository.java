package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {
    List<Question> findAllByUser(User user);

    Page<Question> findAllByUserAndHiddenFalse(User user, Pageable pageable);

    /** Questions matching a web-style query ("rice blast" -wheat), best match first; same text setup as blog search. */
    @Query("""
            select q from Question q
            where q.hidden = false and function('ts_match_vq',
                           function('to_tsvector', 'simple', concat(coalesce(q.title, ''), ' ', coalesce(q.content, ''))),
                           function('websearch_to_tsquery', 'simple', :query)) = true
            order by function('ts_rank',
                              function('to_tsvector', 'simple', concat(coalesce(q.title, ''), ' ', coalesce(q.content, ''))),
                              function('websearch_to_tsquery', 'simple', :query)) desc, q.id desc
            """)
    Page<Question> search(@Param("query") String query, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Question x SET x.viewCount = x.viewCount + 1 WHERE x.id = :id")
    void addView(@Param("id") Long id);

    /** Atomic, so two votes at once cannot overwrite each other. */
    @Modifying
    @Query("UPDATE Question x SET x.score = x.score + :delta WHERE x.id = :id")
    void addToScore(@Param("id") Long id, @Param("delta") long delta);

    long countByUserId(Long userId);

    long countByUserIdAndHiddenFalse(Long userId);

    Page<Question> findAllByHiddenFalseAndCreationDateAfter(LocalDateTime since, Pageable pageable);

    long countByCreationDateAfter(LocalDateTime since);
}
