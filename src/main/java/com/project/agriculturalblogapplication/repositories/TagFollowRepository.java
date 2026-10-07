package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.TagFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TagFollowRepository extends JpaRepository<TagFollow, Long> {

    boolean existsByUserIdAndTagId(Long userId, Long tagId);

    @Modifying
    @Transactional
    void deleteByUserIdAndTagId(Long userId, Long tagId);

    Page<TagFollow> findAllByUserId(Long userId, Pageable pageable);
}
