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

    long countByHiddenFalse();

    /** Visible questions with at least one visible direct answer. */
    @Query("select count(q) from Question q where q.hidden = false and exists (select 1 from Answer a"
            + " where a.question = q and a.parentAnswer is null and a.hidden = false)")
    long countAnswered();

    Page<Question> findAllByHiddenFalseAndCreationDateAfter(LocalDateTime since, Pageable pageable);

    long countByCreationDateAfter(LocalDateTime since);

    @Modifying
    @Transactional
    @Query("update Question q set q.answerMailPending = true where q.id = :id")
    void markAnswerMailPending(@Param("id") Long id);

    /** Takes the pending mail for this sender, so two threads never send the same one. 1 when it was ours. */
    @Modifying
    @Transactional
    @Query("update Question q set q.answerMailPending = false, q.answerMailedAt = :now"
            + " where q.id = :id and q.answerMailPending = true and q.answerMailedAt is null")
    int claimFirstAnswerMail(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("update Question q set q.answerMailPending = false, q.answerMailedAt = :now"
            + " where q.id = :id and q.answerMailPending = true and q.answerMailedAt = :previous")
    int claimNextAnswerMail(@Param("id") Long id, @Param("previous") LocalDateTime previous, @Param("now") LocalDateTime now);

    /** Pending mails whose hour has passed. */
    @Query("select q.id from Question q where q.answerMailPending = true"
            + " and (q.answerMailedAt is null or q.answerMailedAt < :before)")
    List<Long> answerMailsDue(@Param("before") LocalDateTime before);

    /** Views on the public questions this user answered (each question once). */
    @Query("select coalesce(sum(q.viewCount), 0) from Question q where q.hidden = false and exists"
            + " (select 1 from Answer a where a.question = q and a.user.id = :userId and a.parentAnswer is null and a.hidden = false)")
    long sumViewsOfQuestionsAnsweredBy(@Param("userId") Long userId);

    /** Questions where this user's answer is vetted: accepted by someone else, or theirs in a topic they are expert in. */
    @Query("select q.id from Question q where q.hidden = false and q.acceptedAnswer.user.id = :userId"
            + " and q.user.id <> :userId and q.acceptedAnswer.hidden = false")
    List<Long> idsWithAnswerAcceptedFrom(@Param("userId") Long userId);

    @Query("select distinct a.question.id from Answer a where a.user.id = :userId and a.parentAnswer is null"
            + " and a.hidden = false and a.question.hidden = false and a.question.category.id in :topicIds")
    List<Long> idsAnsweredInTopics(@Param("userId") Long userId, @Param("topicIds") java.util.Collection<Long> topicIds);

    /** Unanswered public questions with any of these tags since a moment, newest first; not the reader's own. */
    @Query("select distinct q from Question q join q.tags t where t.id in :tagIds and q.hidden = false"
            + " and q.answerCount = 0 and q.creationDate >= :since and q.user.id <> :readerId order by q.creationDate desc")
    List<Question> unansweredWithTagsSince(@Param("tagIds") java.util.Collection<Long> tagIds,
                                           @Param("since") LocalDateTime since, @Param("readerId") Long readerId,
                                           org.springframework.data.domain.Pageable pageable);
}
