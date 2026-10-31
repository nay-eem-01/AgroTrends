package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.model.AgriInfo;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.util.SafeHtml;
import com.project.agriculturalblogapplication.util.Slugs;

import java.time.Instant;
import java.util.List;

/** A blog in a list (feeds, search, bookmarks, drafts): a plain-text excerpt instead of the full body. */
public record BlogSummaryResponse(
        Long id,
        String slug,
        String title,
        String subtitle,
        String excerpt,
        String imageUrl,
        int readingTimeMinutes,
        List<String> tags,
        long clapCount,
        AgriInfo agri,
        CategoryResponse category,
        AuthorSummaryResponse author,
        BlogStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean aiCitable
) {

    static final int EXCERPT_CHARS = 200;

    public static BlogSummaryResponse from(Blog blog) {
        return new BlogSummaryResponse(blog.getId(), blog.getSlug(), blog.getTitle(), blog.getSubtitle(), excerpt(blog.getContent()),
                blog.getImageUrl(), Slugs.readingTimeMinutes(blog.getContent()),
                blog.getTags() == null ? List.of() : blog.getTags().stream().map(Tag::getName).sorted().toList(),
                blog.getClapCount(),
                AgriInfo.from(blog.getAgri()),
                CategoryResponse.from(blog.getCategory()), AuthorSummaryResponse.from(blog.getAuthor()),
                blog.getStatus(), CommonUtils.toInstant(blog.getPublishedAt()),
                CommonUtils.toInstant(blog.getCreationDate()), CommonUtils.toInstant(blog.getLastModifiedDate()),
                blog.isAiCitable());
    }

    static String excerpt(String content) {
        String text = SafeHtml.plainText(content);
        if (text.length() <= EXCERPT_CHARS) {
            return text;
        }
        int cut = text.lastIndexOf(' ', EXCERPT_CHARS);
        return text.substring(0, cut > EXCERPT_CHARS / 2 ? cut : EXCERPT_CHARS).stripTrailing() + "…";
    }
}
