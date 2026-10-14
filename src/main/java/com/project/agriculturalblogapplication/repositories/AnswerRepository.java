package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestionIdAndParentAnswerIsNull(Long questionId);

    List<Answer> findAllByUserIdAndQuestionCategoryIdAndParentAnswerIsNull(Long userId, Long categoryId);

    /** Atomic, so two votes at once cannot overwrite each other. */
    @Modifying
    @Query("UPDATE Answer x SET x.score = x.score + :delta WHERE x.id = :id")
    void addToScore(@Param("id") Long id, @Param("delta") long delta);
}
