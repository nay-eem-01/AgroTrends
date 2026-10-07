package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AiProperties;
import com.project.agriculturalblogapplication.entities.AiAnswer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.AiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.AiDraftAnswerResponse;
import com.project.agriculturalblogapplication.model.response.AiHistoryItemResponse;
import com.project.agriculturalblogapplication.model.response.AiSourceResponse;
import com.project.agriculturalblogapplication.repositories.AiRepositories;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceTest {

    private final AiRepositories aiRepositories = mock(AiRepositories.class);
    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final AiProperties aiProperties = new AiProperties();
    private final QuestionService questionService = mock(QuestionService.class);
    private final AnswerService answerService = mock(AnswerService.class);
    private final AiService aiService =
            new AiService(aiRepositories, chatClient, authorization, aiProperties, questionService, answerService);

    @Test
    void askReturnsTheAnswerWithItsSourcesAndStoresItForTheCaller() {
        when(authorization.currentUserId("en")).thenReturn(7L);
        when(chatClient.prompt().user("How do I stop rice blast?").call().chatClientResponse())
                .thenReturn(response("Use resistant varieties.", List.of(chunk(2, "Rice blast in short"))));

        AiAnswerResponse answer = aiService.ask("How do I stop rice blast?", "en");

        assertEquals("Use resistant varieties.", answer.answer());
        assertEquals(List.of(new AiSourceResponse(2L, "Rice blast in short")), answer.sources());
        ArgumentCaptor<AiAnswer> saved = ArgumentCaptor.forClass(AiAnswer.class);
        verify(aiRepositories).save(saved.capture());
        assertEquals(7L, saved.getValue().getUserId());
        assertEquals("Use resistant varieties.", saved.getValue().getAiAnswer());
        assertEquals(120, saved.getValue().getPromptTokens());
        assertEquals(30, saved.getValue().getCompletionTokens());
    }

    @Test
    void askIsRefusedOnceTheDailyLimitIsReachedWithoutCallingTheModel() {
        aiProperties.setDailyQuestionLimit(20);
        when(authorization.currentUserId("en")).thenReturn(7L);
        when(aiRepositories.countByUserIdAndCreationDateGreaterThanEqual(7L, LocalDate.now().atStartOfDay())).thenReturn(20L);

        ApplicationException e = assertThrows(ApplicationException.class, () -> aiService.ask("Another question", "en"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
        verify(chatClient, never()).prompt();
        verify(aiRepositories, never()).save(any());
    }

    @Test
    void aFailedModelCallIsA503AndIsNotStored() {
        when(authorization.currentUserId("en")).thenReturn(7L);
        when(chatClient.prompt().user("Slow question").call().chatClientResponse())
                .thenThrow(new RuntimeException("Read timed out"));

        ApplicationException e = assertThrows(ApplicationException.class, () -> aiService.ask("Slow question", "en"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getHttpStatus());
        verify(aiRepositories, never()).save(any());
    }

    @Test
    void severalChunksOfOnePostBecomeOneSourceInRetrievalOrder() {
        List<AiSourceResponse> sources = AiService.sourcesOf(List.of(
                chunk(5, "Feeding dairy cows"), chunk(2, "Rice blast in short"), chunk(5, "Feeding dairy cows")));

        assertEquals(List.of(new AiSourceResponse(5L, "Feeding dairy cows"), new AiSourceResponse(2L, "Rice blast in short")),
                sources);
    }

    @Test
    void noRetrievedPostsMeansNoSources() {
        assertEquals(List.of(), AiService.sourcesOf(null));
        assertEquals(List.of(), AiService.sourcesOf(List.of()));
        assertEquals(List.of(), AiService.sourcesOf(List.of(new Document("chunk without a blog id"))));
    }

    @Test
    void historyIsTheCallersOwnNewestFirstWithAClampedPageSize() {
        when(authorization.currentUserId("en")).thenReturn(7L);
        AiAnswer stored = new AiAnswer();
        stored.setId(3L);
        stored.setQuestion("How do I stop rice blast?");
        stored.setAiAnswer("Use resistant varieties.");
        stored.setCreationDate(LocalDateTime.of(2026, 10, 6, 12, 0));
        when(aiRepositories.findAllByUserId(eq(7L), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(stored), call.getArgument(1), 1));

        Page<AiHistoryItemResponse> page = aiService.history(-1, 500, "en");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(aiRepositories).findAllByUserId(eq(7L), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "creationDate"), pageable.getValue().getSort());
        AiHistoryItemResponse item = page.getContent().get(0);
        assertEquals("Use resistant varieties.", item.answer());
        assertEquals(LocalDateTime.of(2026, 10, 6, 12, 0).atZone(ZoneId.systemDefault()).toInstant(), item.askedAt());
    }

    @Test
    void draftAnswerAsksTheModelAndLabelsTheResult() {
        when(questionService.findByIdWithException(4L)).thenReturn(question("Yellow leaves", "My paddy leaves turn yellow."));
        when(authorization.currentUserId("en")).thenReturn(7L);
        when(chatClient.prompt().user("Yellow leaves\n\nMy paddy leaves turn yellow.").call().chatClientResponse())
                .thenReturn(response("Check nitrogen.", List.of(chunk(2, "Rice nutrition"))));

        AiDraftAnswerResponse draft = aiService.draftAnswer(4L, "en");

        assertEquals(AiService.AI_DRAFT_LABEL, draft.label());
        assertEquals("Check nitrogen.", draft.answer());
        assertEquals(List.of(new AiSourceResponse(2L, "Rice nutrition")), draft.sources());
    }

    @Test
    void answeredQuestionsGetNoDraft() {
        when(questionService.findByIdWithException(4L)).thenReturn(question("Yellow leaves", "Why?"));
        when(answerService.hasAnswers(4L)).thenReturn(true);

        ApplicationException e = assertThrows(ApplicationException.class, () -> aiService.draftAnswer(4L, "en"));

        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
        verify(chatClient, never()).prompt();
    }

    private static Question question(String title, String content) {
        Question question = new Question();
        question.setTitle(title);
        question.setContent(content);
        return question;
    }

    // Metadata read back from pgvector JSON holds Integers, so ids are taken as any Number.
    private static Document chunk(Integer blogId, String title) {
        return new Document("excerpt", Map.of(DocumentService.BLOG_ID, blogId, DocumentService.TITLE, title));
    }

    private static ChatClientResponse response(String answer, List<Document> retrieved) {
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(answer))),
                ChatResponseMetadata.builder().usage(new DefaultUsage(120, 30)).build());
        return new ChatClientResponse(chatResponse, Map.of(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, retrieved));
    }
}
