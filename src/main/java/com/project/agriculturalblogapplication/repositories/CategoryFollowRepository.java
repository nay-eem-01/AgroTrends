package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.CategoryFollow;
import com.project.agriculturalblogapplication.model.response.CategoryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CategoryFollowRepository extends JpaRepository<CategoryFollow, CategoryFollow.Key> {

    @Modifying
    @Transactional
    @Query(value = """
            insert into category_follows (user_id, category_id, followed_at)
            values (:userId, :categoryId, current_timestamp)
            on conflict (user_id, category_id) do nothing
            """, nativeQuery = true)
    void follow(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    @Modifying
    @Transactional
    @Query("delete from CategoryFollow f where f.userId = :userId and f.categoryId = :categoryId")
    void unfollow(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    @Query("""
            select new com.project.agriculturalblogapplication.model.response.CategoryResponse(c.id, c.categoryName)
            from CategoryFollow f join f.category c where f.userId = :userId
            order by f.followedAt desc, c.id
            """)
    List<CategoryResponse> followed(@Param("userId") Long userId);
}
