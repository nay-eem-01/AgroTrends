package com.project.agriculturalblogapplication.Repositories;

import com.project.agriculturalblogapplication.Models.Blogs;
import com.project.agriculturalblogapplication.Models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepositories extends JpaRepository<Users, Long> {
    Optional<Users> findByUsersEmail(String email);
}
