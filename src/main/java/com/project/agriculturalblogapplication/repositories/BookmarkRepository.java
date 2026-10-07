package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Bookmark;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserIdAndBlogId(Long userId, Long blogId);

    @Modifying
    @Transactional
    void deleteByUserIdAndBlogId(Long userId, Long blogId);

    Page<Bookmark> findAllByUserIdAndBlogStatus(Long userId, BlogStatus status, Pageable pageable);
}
