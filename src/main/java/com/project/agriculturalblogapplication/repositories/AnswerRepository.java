package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
