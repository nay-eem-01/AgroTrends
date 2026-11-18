package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.AuthorFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AuthorFollowRepository extends JpaRepository<AuthorFollow, Long> {

    boolean existsByUserIdAndAuthorId(Long userId, Long authorId);

    @Modifying
    @Transactional
    void deleteByUserIdAndAuthorId(Long userId, Long authorId);

    Page<AuthorFollow> findAllByUserId(Long userId, Pageable pageable);

    long countByAuthorId(Long authorId);
}
