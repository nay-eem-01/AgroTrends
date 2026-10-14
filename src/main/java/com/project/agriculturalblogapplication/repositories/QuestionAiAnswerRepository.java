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
import java.util.Optional;

public interface QuestionAiAnswerRepository extends JpaRepository<QuestionAiAnswer, Long> {

    @EntityGraph(attributePaths = {"question", "reviewedBy"})
    Optional<QuestionAiAnswer> findByQuestionId(Long questionId);

    long countByQuestionUserIdAndCreationDateGreaterThanEqual(Long userId, LocalDateTime since);

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
