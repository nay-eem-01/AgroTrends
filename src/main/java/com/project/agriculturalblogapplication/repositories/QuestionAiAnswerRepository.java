package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionAiAnswerRepository extends JpaRepository<QuestionAiAnswer, Long> {

    @EntityGraph(attributePaths = {"question", "reviewedBy"})
    Optional<QuestionAiAnswer> findByQuestionId(Long questionId);

    boolean existsByQuestionId(Long questionId);

    /** Questions whose AI answer this user confirmed in this topic (to reopen when their badge is revoked). */
    @Query("select a.question.id from QuestionAiAnswer a where a.reviewedBy.id = :userId and a.question.category.id = :topicId"
            + " and a.status = :status")
    List<Long> reviewedBy(@Param("userId") Long userId, @Param("topicId") Long topicId, @Param("status") AiAnswerStatus status);

    /** Puts these AI answers back to UNCHECKED. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update QuestionAiAnswer a set a.status = com.project.agriculturalblogapplication.enums.AiAnswerStatus.UNCHECKED,
                   a.reviewedBy = null, a.reviewedAt = null, a.correction = null
            where a.question.id in :questionIds""")
    int reopen(@Param("questionIds") Collection<Long> questionIds);

    /** Puts the AI answer this answer corrected back to UNCHECKED: without the correction it needs a new review. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update QuestionAiAnswer a set a.status = com.project.agriculturalblogapplication.enums.AiAnswerStatus.UNCHECKED,
                   a.reviewedBy = null, a.reviewedAt = null, a.correction = null
            where a.correction.id = :answerId""")
    int reopenCorrectedBy(@Param("answerId") Long answerId);

    @EntityGraph(attributePaths = "question")
    Page<QuestionAiAnswer> findAllByStatusAndQuestionCategoryIdIn(AiAnswerStatus status, Collection<Long> topicIds,
                                                                 Pageable pageable);

    /** Moves the review state only if it is still in {@code from}: two reviewers at once cannot both win. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update QuestionAiAnswer a set a.status = :to, a.reviewedBy = :reviewer, a.reviewedAt = :reviewedAt,
                   a.correction = :correction
            where a.id = :id and a.status in :from""")
    int transition(@Param("id") Long id, @Param("from") Collection<AiAnswerStatus> from, @Param("to") AiAnswerStatus to,
                   @Param("reviewer") User reviewer, @Param("reviewedAt") LocalDateTime reviewedAt,
                   @Param("correction") Answer correction);
}
