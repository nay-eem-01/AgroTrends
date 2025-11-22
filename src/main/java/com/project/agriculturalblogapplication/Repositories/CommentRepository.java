package com.project.agriculturalblogapplication.Repositories;

import com.project.agriculturalblogapplication.entities.Comments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comments,Long> {

    List<Comments> findByBlogIdAndParentCommentIsNull(Long blogId);

    List<Comments> findByParentCommentId(Long parentCommentId);
}
