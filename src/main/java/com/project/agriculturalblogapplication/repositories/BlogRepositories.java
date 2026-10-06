package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Read-only transaction for every query method: one consistent snapshot per call. (It was added when
// Blog.content was a large object that could only be read inside a transaction; 2.5 made it TEXT.)
@Repository
@Transactional(readOnly = true)
public interface BlogRepositories extends JpaRepository<Blog, Long> {

    Blog findBlogsById(Long blogId);

    Page<Blog> findAllByAuthorAndStatus(Author author, BlogStatus status, Pageable pageable);

    Page<Blog> findAllByCategoryAndStatus(Category category, BlogStatus status, Pageable pageable);

    Page<Blog> findAllByStatus(BlogStatus status, Pageable pageable);

    long countByAuthorIdAndStatus(Long authorId, BlogStatus status);

    /** Published blogs by authors the user follows or carrying a tag the user follows. */
    @Query("""
            SELECT b FROM Blog b
            WHERE b.status = com.project.agriculturalblogapplication.enums.BlogStatus.PUBLISHED
              AND (EXISTS (SELECT f FROM AuthorFollow f WHERE f.user.id = :userId AND f.author = b.author)
                   OR EXISTS (SELECT tf FROM TagFollow tf WHERE tf.user.id = :userId AND tf.tag MEMBER OF b.tags))
            """)
    Page<Blog> findFollowingFeed(@Param("userId") Long userId, Pageable pageable);

    Page<Blog> findAllByStatusAndPublishedAtAfter(BlogStatus status, LocalDateTime since, Pageable pageable);

    /** Atomic, so two readers clapping at once cannot overwrite each other's count. */
    @Modifying
    @Transactional
    @Query("UPDATE Blog b SET b.clapCount = b.clapCount + :delta WHERE b.id = :blogId")
    void addClaps(@Param("blogId") Long blogId, @Param("delta") long delta);

    Page<Blog> findAllByTagsNameAndStatus(String tagName, BlogStatus status, Pageable pageable);

    Optional<Blog> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Blog> findAllBySlugIsNull();

    /** Published blogs matching a web-style query ("rice blast" -wheat), best match first. */
    @Query(value = """
            SELECT b.* FROM blogs b
            WHERE b.status = 'PUBLISHED'
              AND to_tsvector('simple', coalesce(b.title, '') || ' ' || coalesce(b.content, ''))
                  @@ websearch_to_tsquery('simple', :query)
            ORDER BY ts_rank(to_tsvector('simple', coalesce(b.title, '') || ' ' || coalesce(b.content, '')),
                             websearch_to_tsquery('simple', :query)) DESC, b.id DESC
            """,
            countQuery = """
            SELECT count(*) FROM blogs b
            WHERE b.status = 'PUBLISHED'
              AND to_tsvector('simple', coalesce(b.title, '') || ' ' || coalesce(b.content, ''))
                  @@ websearch_to_tsquery('simple', :query)
            """,
            nativeQuery = true)
    Page<Blog> search(@Param("query") String query, Pageable pageable);
}
