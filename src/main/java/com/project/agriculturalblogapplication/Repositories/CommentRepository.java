package com.project.agriculturalblogapplication.Repositories;

import com.project.agriculturalblogapplication.Models.Comments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comments,Long> {

    // Fetch top-level comments (those without parent)
    List<Comments> findByBlogBlogIdAndParentCommentIsNull(Long blogId);

    // Fetch replies for a given comment
    List<Comments> findByParentCommentCommentId(Long parentCommentId);


}
