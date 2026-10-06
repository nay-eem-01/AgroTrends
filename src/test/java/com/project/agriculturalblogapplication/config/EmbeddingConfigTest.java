package com.project.agriculturalblogapplication.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EmbeddingConfigTest {

    private static final String MODEL = "spring.ai.google.genai.embedding.text.options.model";
    private static final String EMBEDDING_DIMENSIONS = "spring.ai.google.genai.embedding.text.options.dimensions";
    private static final String VECTOR_DIMENSIONS = "spring.ai.vectorstore.pgvector.dimensions";

    private final Properties properties = load();

    @Test
    void doesNotUseTheShutDownEmbeddingModel() {
        // Regression: text-embedding-004 was shut down on 2026-01-14, so every blog create/update failed.
        assertNotEquals("text-embedding-004", properties.getProperty(MODEL));
    }

    @Test
    void embeddingDimensionsMatchTheVectorTable() {
        // gemini-embedding models default to 3072; the vector_store column is vector(768).
        assertEquals("768", properties.getProperty(EMBEDDING_DIMENSIONS));
        assertEquals(properties.getProperty(EMBEDDING_DIMENSIONS), properties.getProperty(VECTOR_DIMENSIONS));
    }

    private static Properties load() {
        try {
            return PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
