package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepositories extends JpaRepository<Blog, Long> {

    Blog findBlogsById(Long blogId);

    List<Blog> findBlogsByCategory(Categories category);

    List<Blog> findBlogsByAuthor(Author author);

}
