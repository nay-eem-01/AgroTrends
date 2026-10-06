package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AIConfig;
import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DocumentServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final DocumentService documentService = new DocumentService(vectorStore, new AIConfig().blogTextSplitter());

    @Test
    void shortBlogIsOneChunkWithItsMetadata() {
        documentService.indexBlog(blog(1L, "Rice blast is a fungal disease of paddy."));

        List<Document> chunks = indexedChunks();
        assertEquals(1, chunks.size());
        assertMetadata(chunks.get(0), 1L);
        assertTrue(chunks.get(0).getText().startsWith("Managing rice blast"));
    }

    @Test
    void longBlogIsSplitAndEveryChunkKeepsTheMetadata() {
        String longContent = "Apply balanced nitrogen and potassium to reduce blast severity in rice. ".repeat(400);

        documentService.indexBlog(blog(2L, longContent));

        List<Document> chunks = indexedChunks();
        assertTrue(chunks.size() > 1, "expected several chunks, got " + chunks.size());
        chunks.forEach(chunk -> assertMetadata(chunk, 2L));
    }

    @Test
    void reindexDeletesTheBlogsOldChunksBeforeAddingNewOnes() {
        documentService.reindexBlog(blog(3L, "Updated advice."));

        InOrder inOrder = inOrder(vectorStore);
        inOrder.verify(vectorStore).delete(blogFilter(3L));
        inOrder.verify(vectorStore).add(anyList());
    }

    @Test
    void deleteRemovesOnlyThatBlogsChunks() {
        documentService.deleteBlog(4L);

        verify(vectorStore).delete(blogFilter(4L));
        verify(vectorStore, never()).add(anyList());
    }

    private static Filter.Expression blogFilter(Long blogId) {
        return new FilterExpressionBuilder().eq(DocumentService.BLOG_ID, blogId).build();
    }

    @SuppressWarnings("unchecked")
    private List<Document> indexedChunks() {
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        return captor.getValue();
    }

    private static void assertMetadata(Document chunk, Long blogId) {
        assertEquals(blogId, chunk.getMetadata().get(DocumentService.BLOG_ID));
        assertEquals(20L, chunk.getMetadata().get(DocumentService.AUTHOR_ID));
        assertEquals(30L, chunk.getMetadata().get(DocumentService.CATEGORY_ID));
        assertEquals("Managing rice blast", chunk.getMetadata().get(DocumentService.TITLE));
    }

    private static Blog blog(Long id, String content) {
        Author author = new Author();
        author.setId(20L);
        Category category = new Category();
        category.setId(30L);

        Blog blog = new Blog();
        blog.setId(id);
        blog.setTitle("Managing rice blast");
        blog.setContent(content);
        blog.setAuthor(author);
        blog.setCategory(category);
        return blog;
    }
}
