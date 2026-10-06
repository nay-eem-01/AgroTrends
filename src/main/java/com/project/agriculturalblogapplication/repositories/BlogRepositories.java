package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepositories extends JpaRepository<Blog, Long> {

    Blog findBlogsById(Long blogId);

    Page<Blog> findAllByAuthorAndStatus(Author author, BlogStatus status, Pageable pageable);

    Page<Blog> findAllByCategoryAndStatus(Category category, BlogStatus status, Pageable pageable);

    Page<Blog> findAllByStatus(BlogStatus status, Pageable pageable);
}
