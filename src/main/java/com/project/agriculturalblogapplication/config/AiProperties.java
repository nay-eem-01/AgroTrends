package com.project.agriculturalblogapplication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiProperties {

    /** Questions one user may ask /api/ai/ask per calendar day (server time zone). */
    private int dailyQuestionLimit = 20;

    /** Upper bound for one call to Gemini. */
    private Duration timeout = Duration.ofSeconds(60);

    private final Rag rag = new Rag();

    /** Minimum cosine similarity between two posts for "related posts" (unrelated farming posts measure 0.64-0.76). */
    private double relatedSimilarityThreshold = 0.75;

    @Getter
    @Setter
    public static class Rag {

        /** How many blog chunks go into the prompt. */
        private int topK = 5;

        /** Minimum cosine similarity (0..1) for a chunk to count as relevant. */
        private double similarityThreshold = 0.7;
    }
}
