package com.project.agriculturalblogapplication.config;

import com.project.agriculturalblogapplication.model.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.data.domain.AuditorAware;

// Async: AI answers on new questions are generated off the request thread (AiService.answerQuestion).
@EnableAsync
@Configuration
public class AppConfig {

    /**
     * One thread for AI index updates (AnswerIndexer): changes to the same answer apply in the order they were
     * made, and a slow embedding call holds one connection, off the request. spring.task.execution.mode=force keeps
     * Boot's default executor for other @Async work alongside this bean.
     */
    @Bean
    public ThreadPoolTaskExecutor indexingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("indexing-");
        return executor;
    }

    @Bean
    public AuditorAware<String> auditorAware(){
        return new AuditorAwareImpl();
    }
}
