package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AiProperties;
import com.project.agriculturalblogapplication.entities.Blog;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentService {

    public static final String BLOG_ID = "blogId";
    public static final String AUTHOR_ID = "authorId";
    public static final String CATEGORY_ID = "categoryId";
    public static final String TITLE = "title";

    // A post's opening is enough to find its neighbours and keeps the query embedding cheap.
    private static final int RELATED_QUERY_MAX_CHARS = 2000;
    // Several chunks can belong to one post, so fetch more chunks than posts wanted.
    private static final int CHUNKS_PER_RELATED_POST = 4;

    private final VectorStore vectorStore;

    private final TextSplitter blogTextSplitter;

    private final AiProperties aiProperties;

    public void indexBlog(Blog blog) {
        // The splitter copies the blog's metadata onto every chunk, so any chunk can be traced back to its post.
        vectorStore.add(blogTextSplitter.apply(List.of(toDocument(blog))));
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
                .filterExpression(new FilterExpressionBuilder().ne(BLOG_ID, blog.getId()).build())
                .build());
        return chunks.stream()
                .map(chunk -> chunk.getMetadata().get(BLOG_ID))
                .filter(Number.class::isInstance)
                .map(id -> ((Number) id).longValue())
                .distinct()
                .limit(limit)
                .toList();
    }

    private static Document toDocument(Blog blog) {
        return new Document(blog.getTitle() + "\n\n" + blog.getContent(), Map.of(
                BLOG_ID, blog.getId(),
                AUTHOR_ID, blog.getAuthor().getId(),
                CATEGORY_ID, blog.getCategory().getId(),
                TITLE, blog.getTitle()));
    }
}
