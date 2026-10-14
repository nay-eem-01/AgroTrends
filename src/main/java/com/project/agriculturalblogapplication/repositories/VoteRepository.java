package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByUserIdAndQuestionId(Long userId, Long questionId);

    Optional<Vote> findByUserIdAndAnswerId(Long userId, Long answerId);

    /** The user's votes on a question and on all of its answers. */
    @Query("""
            select v from Vote v left join v.answer a
            where v.user.id = :userId and (v.question.id = :questionId or a.question.id = :questionId)
            """)
    List<Vote> findOnThread(@Param("userId") Long userId, @Param("questionId") Long questionId);
}
