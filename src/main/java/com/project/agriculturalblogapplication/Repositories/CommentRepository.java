package com.project.agriculturalblogapplication.Repositories;

import com.project.agriculturalblogapplication.Models.Comments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comments,Long> {
}
