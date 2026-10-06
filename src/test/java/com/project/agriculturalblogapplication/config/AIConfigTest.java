package com.project.agriculturalblogapplication.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIConfigTest {

    @Test
    void chatClientUsesTheSystemPrompt() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        when(builder.defaultSystem(AIConfig.SYSTEM_PROMPT)).thenReturn(builder);
        when(builder.build()).thenReturn(client);

        assertSame(client, new AIConfig().chatClient(builder));
        verify(builder).defaultSystem(AIConfig.SYSTEM_PROMPT);
    }

    @Test
    void systemPromptAnswersEnglishQuestionsInEnglish() {
        // Regression: the prompt only mentioned Bangla, so English questions were sometimes answered in Bangla.
        assertTrue(AIConfig.SYSTEM_PROMPT.contains("English for a question in English"));
        assertTrue(AIConfig.SYSTEM_PROMPT.contains("Bangla for a question in Bangla"));
    }
}
