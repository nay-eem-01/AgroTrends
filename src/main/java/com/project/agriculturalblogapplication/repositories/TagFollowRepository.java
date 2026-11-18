package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.TagFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TagFollowRepository extends JpaRepository<TagFollow, Long> {

    boolean existsByUserIdAndTagId(Long userId, Long tagId);

    @Modifying
    @Transactional
    void deleteByUserIdAndTagId(Long userId, Long tagId);

    Page<TagFollow> findAllByUserId(Long userId, Pageable pageable);

    @Query("select f.tag.id from TagFollow f where f.user.id = :userId")
    List<Long> followedTagIds(@Param("userId") Long userId);
}
