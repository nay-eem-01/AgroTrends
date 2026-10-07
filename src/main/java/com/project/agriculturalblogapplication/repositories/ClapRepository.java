package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Clap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClapRepository extends JpaRepository<Clap, Long> {

    Optional<Clap> findByBlogIdAndUserId(Long blogId, Long userId);
}
