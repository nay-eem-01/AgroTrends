package com.project.agriculturalblogapplication.config;

import com.project.agriculturalblogapplication.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serves uploaded images read-only at /uploads/** (public, like the posts that embed them). */
@Configuration
@RequiredArgsConstructor
public class UploadsResourceConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(Path.of(storageProperties.getLocalDir()).toAbsolutePath().toUri().toString());
    }
}
