package com.project.agriculturalblogapplication.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    static final String SYSTEM_PROMPT = "You are an expert agricultural advisor. "
            + "Answer in the language of the question: Bangla for a question in Bangla, English for a question in English. "
            + "Answer in Bangla only if the user explicitly asks for it.";

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem(SYSTEM_PROMPT)
            .build();
    }

    /**
     * Splits blog text into chunks of about 800 tokens before embedding, so long posts are not cut off at the
     * embedding model's input limit and retrieval can point at the relevant part of a post.
     */
    @Bean
    public TextSplitter blogTextSplitter() {
        return TokenTextSplitter.builder()
            .withChunkSize(800)
            .build();
    }
}
