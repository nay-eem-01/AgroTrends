package com.project.agriculturalblogapplication.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.validation.annotation.Validated;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {

    private String name;

    private String backendUrl;

    private String backendUrlShort;

    @Valid
    private Moderation moderation = new Moderation();

    @Getter
    @Setter
    public static class Moderation {

        /** Open reports from different users that hide a post until a moderator decides; 0 would let one report hide anything. */
        @Min(1)
        private int autoHideReports = 3;
    }

}
