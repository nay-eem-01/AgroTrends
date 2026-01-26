package com.project.agriculturalblogapplication.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {
    
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem("You are an expert agricultural advisor. Answer in Bangla if the question is in Bangla.")
            .build();
    }
}