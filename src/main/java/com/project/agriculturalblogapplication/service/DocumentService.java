package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Blog;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VectorStore vectorStore;

    public void indexBlog(Blog blog) {
        String content = blog.getTitle() + "\n" + blog.getContent();
        Document doc = new Document(content, Map.of("blogId", blog.getId()));
        vectorStore.add(List.of(doc));
    }

    public void indexAllBlogs(List<Blog> blogs) {
        List<Document> documents = blogs.stream()
                .map(blog -> new Document(
                        blog.getTitle() + "\n" + blog.getContent(),
                        Map.of("blogId", blog.getId())
                ))
                .toList();

        vectorStore.add(documents);
    }
}
