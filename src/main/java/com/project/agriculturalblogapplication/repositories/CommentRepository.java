package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {

    List<Comment> findByBlogIdAndParentCommentIsNullAndHiddenFalse(Long blogId);

    List<Comment> findByParentCommentIdAndHiddenFalseOrderByIdAsc(Long parentCommentId);

    /** Pairs of (commentId, visible replies) for these comments; comments without replies are left out. */
    @Query("select c.parentComment.id, count(c) from Comment c where c.parentComment.id in :ids and c.hidden = false"
            + " group by c.parentComment.id")
    List<Object[]> replyCounts(@Param("ids") Collection<Long> ids);
}
