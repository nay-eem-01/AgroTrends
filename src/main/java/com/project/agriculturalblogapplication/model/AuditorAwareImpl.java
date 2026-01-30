package com.project.agriculturalblogapplication.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return Optional.of(SecurityContextHolder.getContext().getAuthentication().getName());
        } else {
            return Optional.of("SYSTEM");
        }
    }
}
