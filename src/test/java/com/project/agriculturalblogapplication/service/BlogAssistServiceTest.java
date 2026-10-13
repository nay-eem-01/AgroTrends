package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.BlogAssistResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogAssistServiceTest {

    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private BlogAssistService service;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.defaultSystem(BlogAssistService.SYSTEM_PROMPT)).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        service = new BlogAssistService(builder);
    }

    @Test
    void returnsTheModelsSummaryAndTags() {
        BlogAssistResponse expected = new BlogAssistResponse("How to stop rice blast.", List.of("rice", "rice blast"));
        when(chatClient.prompt().user(anyString()).call().entity(BlogAssistResponse.class)).thenReturn(expected);

        assertSame(expected, service.assist("Rice blast", "Spray early.", "en"));
    }

    @Test
    void longDraftsAreCutBeforeTheyReachTheModel() {
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt()).thenReturn(request);

        service.assist("Long post", "x".repeat(BlogAssistService.MAX_CONTENT_CHARS + 500), "en");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(request).user(prompt.capture());
        assertEquals("Title: Long post\n\n".length() + BlogAssistService.MAX_CONTENT_CHARS, prompt.getValue().length());
    }

    @Test
    void aModelFailureIsA503() {
        when(chatClient.prompt().user(anyString()).call().entity(BlogAssistResponse.class))
                .thenThrow(new RuntimeException("timeout"));

        ApplicationException e = assertThrows(ApplicationException.class, () -> service.assist("T", "C", "en"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getHttpStatus());
    }
}
