package com.project.agriculturalblogapplication.config;

import com.project.agriculturalblogapplication.model.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.domain.AuditorAware;

// Async: AI answers on new questions are generated off the request thread (AiService.answerQuestion).
@EnableAsync
@Configuration
public class AppConfig {

    @Bean
    public AuditorAware<String> auditorAware(){
        return new AuditorAwareImpl();
    }
}
