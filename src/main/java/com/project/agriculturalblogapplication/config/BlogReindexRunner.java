package com.project.agriculturalblogapplication.config;

import com.project.agriculturalblogapplication.service.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** One-off backfill: with {@code app.ai.reindex-on-startup=true}, re-embeds every blog when the app starts. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.reindex-on-startup", havingValue = "true")
public class BlogReindexRunner implements ApplicationRunner {

    private final BlogService blogService;

    @Override
    public void run(ApplicationArguments args) {
        log.warn("Re-indexing all blogs into the vector store (app.ai.reindex-on-startup=true); turn it off afterwards.");
        long count = blogService.reindexAll();
        log.info("Re-indexed {} blogs.", count);
    }
}
