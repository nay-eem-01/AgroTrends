package com.project.agriculturalblogapplication.Repositories;

import com.project.agriculturalblogapplication.Models.Blogs;
import com.project.agriculturalblogapplication.Models.Categories;
import com.project.agriculturalblogapplication.Models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepositories extends JpaRepository<Blogs, Long> {
    Blogs findBlogsByBlogId(Long blogId);
    List<Blogs> findBlogsByCategory(Categories category);
    List<Blogs> findBlogsByAuthor(Users author);

}
