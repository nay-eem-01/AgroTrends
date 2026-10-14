package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AIConfig;
import com.project.agriculturalblogapplication.config.AiProperties;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final DocumentService documentService = new DocumentService(vectorStore, new AIConfig().blogTextSplitter(), new AiProperties());

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
    void embeddingFailureIsSwallowedSoTheSavedPostIsNotReportedAsFailed() {
        doThrow(new RuntimeException("Gemini unavailable")).when(vectorStore).add(anyList());

        assertDoesNotThrow(() -> documentService.indexBlog(blog(5L, "Saved even when Gemini is down.")));
        assertDoesNotThrow(() -> documentService.reindexBlog(blog(5L, "Edited while Gemini is down.")));
    }

    @Test
    void deleteRemovesOnlyThatBlogsChunks() {
        documentService.deleteBlog(4L);

        verify(vectorStore).delete(blogFilter(4L));
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void relatedPostsExcludeTheBlogItselfAndListEachPostOnce() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                chunkOf(9), chunkOf(4), chunkOf(9), chunkOf(6)));

        List<Long> related = documentService.findRelatedBlogIds(blog(2L, "Rice blast advice."), 2);

        assertEquals(List.of(9L, 4L), related);
        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        assertEquals(b.and(b.ne(DocumentService.BLOG_ID, 2L), b.eq(DocumentService.STATUS, "PUBLISHED")).build(),
                request.getValue().getFilterExpression());
        assertEquals(0.75, request.getValue().getSimilarityThreshold());
        assertEquals(8, request.getValue().getTopK());
    }

    // pgvector returns metadata numbers as Integers.
    private static Document chunkOf(int blogId) {
        return new Document("excerpt", java.util.Map.of(DocumentService.BLOG_ID, blogId));
    }

    @Test
    void aVettedAnswerReplacesItsChunksWithQuestionTitlePlainTextAndAnswerMetadata() {
        documentService.syncAnswer(answer(), true);

        InOrder order = inOrder(vectorStore);
        order.verify(vectorStore).delete(new FilterExpressionBuilder().eq(DocumentService.ANSWER_ID, 8L).build());
        order.verify(vectorStore).add(anyList());
        Document chunk = indexedChunks().getFirst();
        assertEquals("Yellow leaves\n\nAdd urea in two splits.", chunk.getText());
        assertEquals(8L, chunk.getMetadata().get(DocumentService.ANSWER_ID));
        assertEquals(4L, chunk.getMetadata().get(DocumentService.QUESTION_ID));
        assertEquals(DocumentService.ANSWER_TYPE, chunk.getMetadata().get(DocumentService.TYPE));
        assertEquals("PUBLISHED", chunk.getMetadata().get(DocumentService.STATUS));
        assertEquals(30L, chunk.getMetadata().get(DocumentService.CATEGORY_ID));
    }

    @Test
    void anAnswerThatIsNoLongerVettedIsOnlyRemoved() {
        documentService.syncAnswer(answer(), false);

        verify(vectorStore).delete(new FilterExpressionBuilder().eq(DocumentService.ANSWER_ID, 8L).build());
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void aConfirmedAiAnswerIsIndexedUnderItsOwnIdAndAQuestionDeleteDropsEverythingUnderIt() {
        QuestionAiAnswer aiAnswer = new QuestionAiAnswer();
        aiAnswer.setId(5L);
        aiAnswer.setQuestion(answer().getQuestion());
        aiAnswer.setAnswer("Check nitrogen.");

        documentService.syncAiAnswer(aiAnswer, true);
        documentService.deleteQuestion(4L);

        verify(vectorStore).delete(new FilterExpressionBuilder().eq(DocumentService.AI_ANSWER_ID, 5L).build());
        assertEquals(5L, indexedChunks().getFirst().getMetadata().get(DocumentService.AI_ANSWER_ID));
        verify(vectorStore).delete(new FilterExpressionBuilder().eq(DocumentService.QUESTION_ID, 4L).build());
    }

    @Test
    void aVectorStoreFailureWhileSyncingAnAnswerIsSwallowed() {
        doThrow(new RuntimeException("db down")).when(vectorStore).delete(any(Filter.Expression.class));

        assertDoesNotThrow(() -> documentService.syncAnswer(answer(), true));
    }

    private static Answer answer() {
        Category category = new Category();
        category.setId(30L);
        Question question = new Question();
        question.setId(4L);
        question.setTitle("Yellow leaves");
        question.setCategory(category);
        Answer answer = new Answer();
        answer.setId(8L);
        answer.setQuestion(question);
        answer.setContent("<p>Add <b>urea</b> in two splits.</p>");
        return answer;
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
        assertEquals("PUBLISHED", chunk.getMetadata().get(DocumentService.STATUS));
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
