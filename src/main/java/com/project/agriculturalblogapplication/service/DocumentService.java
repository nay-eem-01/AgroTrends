package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Blog;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
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

    private final VectorStore vectorStore;

    private final TextSplitter blogTextSplitter;

    public void indexBlog(Blog blog) {
        indexAllBlogs(List.of(blog));
    }

    public void indexAllBlogs(List<Blog> blogs) {
        List<Document> documents = blogs.stream()
                .map(DocumentService::toDocument)
                .toList();

        // The splitter copies each blog's metadata onto every chunk, so any chunk can be traced back to its post.
        vectorStore.add(blogTextSplitter.apply(documents));
    }

    private static Document toDocument(Blog blog) {
        return new Document(blog.getTitle() + "\n\n" + blog.getContent(), Map.of(
                BLOG_ID, blog.getId(),
                AUTHOR_ID, blog.getAuthor().getId(),
                CATEGORY_ID, blog.getCategory().getId(),
                TITLE, blog.getTitle()));
    }
}
