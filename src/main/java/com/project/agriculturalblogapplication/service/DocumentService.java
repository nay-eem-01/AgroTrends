package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AiProperties;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    public static final String BLOG_ID = "blogId";
    public static final String AUTHOR_ID = "authorId";
    public static final String CATEGORY_ID = "categoryId";
    public static final String TITLE = "title";
    public static final String STATUS = "status";

    // A post's opening is enough to find its neighbours and keeps the query embedding cheap.
    private static final int RELATED_QUERY_MAX_CHARS = 2000;
    // Several chunks can belong to one post, so fetch more chunks than posts wanted.
    private static final int CHUNKS_PER_RELATED_POST = 4;

    private final VectorStore vectorStore;

    private final TextSplitter blogTextSplitter;

    private final AiProperties aiProperties;

    /**
     * The index is a cache of the post, so an embedding failure (Gemini down, quota hit) is logged, never thrown:
     * the post is already saved and a 500 would make the author retry and create a duplicate.
     * shortcut: a failed post stays out of AI answers until its next edit or a re-index run
     * (AI_REINDEX_ON_STARTUP); add a scheduled retry if these warnings show up in the logs.
     */
    public void indexBlog(Blog blog) {
        // The splitter copies the blog's metadata onto every chunk, so any chunk can be traced back to its post.
        List<Document> chunks = blogTextSplitter.apply(List.of(toDocument(blog)));
        try {
            vectorStore.add(chunks);
        } catch (RuntimeException e) {
            log.warn("Could not index blog {} into the vector store; it is missing from AI answers until re-indexed",
                    blog.getId(), e);
        }
    }

    /** Drops the blog's old chunks before indexing it again, so retrieval never sees an outdated version. */
    public void reindexBlog(Blog blog) {
        deleteBlog(blog.getId());
        indexBlog(blog);
    }

    public void deleteBlog(Long blogId) {
        vectorStore.delete(new FilterExpressionBuilder().eq(BLOG_ID, blogId).build());
    }

    /** Ids of other posts whose chunks are closest to this post, most similar first, each post once. */
    public List<Long> findRelatedBlogIds(Blog blog, int limit) {
        String query = blog.getTitle() + "\n\n" + blog.getContent();
        List<Document> chunks = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query.length() > RELATED_QUERY_MAX_CHARS ? query.substring(0, RELATED_QUERY_MAX_CHARS) : query)
                .topK(limit * CHUNKS_PER_RELATED_POST)
                .similarityThreshold(aiProperties.getRelatedSimilarityThreshold())
                .filterExpression(relatedFilter(blog.getId()))
                .build());
        return chunks.stream()
                .map(chunk -> chunk.getMetadata().get(BLOG_ID))
                .filter(Number.class::isInstance)
                .map(id -> ((Number) id).longValue())
                .distinct()
                .limit(limit)
                .toList();
    }

    /** Retrieval only ever sees published posts, even if a stale draft chunk were left behind. */
    public static Filter.Expression publishedOnly() {
        return new FilterExpressionBuilder().eq(STATUS, BlogStatus.PUBLISHED.name()).build();
    }

    private static Filter.Expression relatedFilter(Long blogId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        return b.and(b.ne(BLOG_ID, blogId), b.eq(STATUS, BlogStatus.PUBLISHED.name())).build();
    }

    private static Document toDocument(Blog blog) {
        return new Document(blog.getTitle() + "\n\n" + blog.getContent(), Map.of(
                BLOG_ID, blog.getId(),
                AUTHOR_ID, blog.getAuthor().getId(),
                CATEGORY_ID, blog.getCategory().getId(),
                TITLE, blog.getTitle(),
                STATUS, blog.getStatus().name()));
    }
}
