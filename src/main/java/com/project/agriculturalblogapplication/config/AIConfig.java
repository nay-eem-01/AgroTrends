package com.project.agriculturalblogapplication.config;

import org.springframework.ai.chat.client.ChatClient;
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
}
