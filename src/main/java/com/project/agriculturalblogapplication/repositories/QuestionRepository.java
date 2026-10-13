package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {
    List<Question> findAllByUser(User user);

    Page<Question> findAllByUser(User user, Pageable pageable);

    /** Questions matching a web-style query ("rice blast" -wheat), best match first; same text setup as blog search. */
    @Query("""
            select q from Question q
            where function('ts_match_vq',
                           function('to_tsvector', 'simple', concat(coalesce(q.title, ''), ' ', coalesce(q.content, ''))),
                           function('websearch_to_tsquery', 'simple', :query)) = true
            order by function('ts_rank',
                              function('to_tsvector', 'simple', concat(coalesce(q.title, ''), ' ', coalesce(q.content, ''))),
                              function('websearch_to_tsquery', 'simple', :query)) desc, q.id desc
            """)
    Page<Question> search(@Param("query") String query, Pageable pageable);
}
