package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.storage.ImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageCleanupTransactionTest {
    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean ImageStorage storage() { return mock(ImageStorage.class); }
        @Bean JdbcTemplate jdbc() { return mock(JdbcTemplate.class); }
        @Bean ImageCleanup cleanup(ImageStorage storage, ApplicationEventPublisher events, JdbcTemplate jdbc) {
            return new ImageCleanup(storage, events, jdbc);
        }
        // In-memory transaction lifecycle: exercise Spring's actual AFTER_COMMIT event dispatch without a DB.
        @Bean AbstractPlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override protected Object doGetTransaction() { return new Object(); }
                @Override protected void doBegin(Object tx, TransactionDefinition definition) {}
                @Override protected void doCommit(DefaultTransactionStatus status) {}
                @Override protected void doRollback(DefaultTransactionStatus status) {}
            };
        }
    }

    @Test
    void aRollbackRetainsTheFileAndACommitDeletesItOnlyAfterCompletion() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            ImageStorage storage = context.getBean(ImageStorage.class);
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            ImageCleanup cleanup = context.getBean(ImageCleanup.class);
            String url = "https://images.example.com/abc.png";
            when(storage.isStored(url)).thenReturn(true);
            when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(url))).thenReturn(false);
            var tx = new TransactionTemplate(context.getBean(AbstractPlatformTransactionManager.class));
            tx.executeWithoutResult(status -> {
                cleanup.consider(url);
                status.setRollbackOnly();
            });
            verify(storage, never()).delete(any());
            tx.executeWithoutResult(status -> {
                cleanup.consider(url);
                verify(storage, never()).delete(any());
            });
            verify(storage).delete(url);
        }
    }
}
