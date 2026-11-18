package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /** Every direct answer, hidden ones included: for the AI index, which must also drop hidden ones. */
    List<Answer> findByQuestionIdAndParentAnswerIsNull(Long questionId);

    List<Answer> findByQuestionIdAndParentAnswerIsNullAndHiddenFalse(Long questionId);

    List<Answer> findByParentAnswerIdAndHiddenFalseOrderByIdAsc(Long parentAnswerId);

    List<Answer> findAllByUserIdAndQuestionCategoryIdAndParentAnswerIsNull(Long userId, Long categoryId);

    /** Atomic, so two votes at once cannot overwrite each other. */
    @Modifying
    @Query("UPDATE Answer x SET x.score = x.score + :delta WHERE x.id = :id")
    void addToScore(@Param("id") Long id, @Param("delta") long delta);

    long countByUserId(Long userId);

    /** A user's public answers: not replies, not hidden, not on a hidden question. */
    Page<Answer> findAllByUserIdAndParentAnswerIsNullAndHiddenFalseAndQuestionHiddenFalse(Long userId, Pageable pageable);

    long countByUserIdAndParentAnswerIsNullAndHiddenFalseAndQuestionHiddenFalse(Long userId);

    @Query("select count(q) from Question q where q.acceptedAnswer.user.id = :userId"
            + " and q.hidden = false and q.acceptedAnswer.hidden = false")
    long countAcceptedByUserId(@Param("userId") Long userId);

    /** Pairs of (answerId, visible replies) for these answers; answers without replies are left out. */
    @Query("select a.parentAnswer.id, count(a) from Answer a where a.parentAnswer.id in :ids and a.hidden = false"
            + " group by a.parentAnswer.id")
    List<Object[]> replyCounts(@Param("ids") Collection<Long> ids);

    /** Answers from others on this question since a moment, oldest first: what the asker has not been e-mailed about. */
    @Query("select a from Answer a where a.question.id = :questionId and a.parentAnswer is null and a.hidden = false"
            + " and a.user.id <> a.question.user.id and a.creationDate > :since order by a.creationDate asc")
    List<Answer> newAnswersSince(@Param("questionId") Long questionId, @Param("since") java.time.LocalDateTime since);
}
