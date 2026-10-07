package com.project.agriculturalblogapplication.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class StorageProperties {

    /** Directory for uploaded images (local-disk storage). */
    private String localDir = "uploads";

    /** Public URL prefix the images are served under, e.g. https://api.example.com/uploads */
    private String publicBaseUrl = "http://localhost:8080/uploads";
}
