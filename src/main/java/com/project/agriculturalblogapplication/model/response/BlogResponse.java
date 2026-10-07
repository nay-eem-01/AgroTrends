package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.util.CommonUtils;

import java.time.Instant;

public record BlogResponse(
        Long id,
        String title,
        String content,
        String imageUrl,
        CategoryResponse category,
        AuthorSummaryResponse author,
        Instant createdAt,
        Instant updatedAt
) {

    public static BlogResponse from(Blog blog) {
        return new BlogResponse(blog.getId(), blog.getTitle(), blog.getContent(), blog.getImageUrl(),
                CategoryResponse.from(blog.getCategory()), AuthorSummaryResponse.from(blog.getAuthor()),
                CommonUtils.toInstant(blog.getCreationDate()), CommonUtils.toInstant(blog.getLastModifiedDate()));
    }
}
