package com.project.agriculturalblogapplication.config;

import com.project.agriculturalblogapplication.service.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Fills slugs for posts created before slugs existed. Idempotent: once all posts have one, it does nothing. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogSlugBackfillRunner implements ApplicationRunner {

    private final BlogService blogService;

    @Override
    public void run(ApplicationArguments args) {
        int filled = blogService.backfillSlugs();
        if (filled > 0) {
            log.info("Gave {} existing blogs a slug.", filled);
        }
    }
}
