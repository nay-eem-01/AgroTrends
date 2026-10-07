package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.util.Slugs;

import java.time.Instant;
import java.util.List;

public record BlogResponse(
        Long id,
        String slug,
        String title,
        String content,
        String imageUrl,
        int readingTimeMinutes,
        List<String> tags,
        CategoryResponse category,
        AuthorSummaryResponse author,
        BlogStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static BlogResponse from(Blog blog) {
        return new BlogResponse(blog.getId(), blog.getSlug(), blog.getTitle(), blog.getContent(), blog.getImageUrl(),
                Slugs.readingTimeMinutes(blog.getContent()),
                blog.getTags() == null ? List.of() : blog.getTags().stream().map(Tag::getName).sorted().toList(),
                CategoryResponse.from(blog.getCategory()), AuthorSummaryResponse.from(blog.getAuthor()),
                blog.getStatus(), CommonUtils.toInstant(blog.getPublishedAt()),
                CommonUtils.toInstant(blog.getCreationDate()), CommonUtils.toInstant(blog.getLastModifiedDate()));
    }
}
