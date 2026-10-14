package com.project.agriculturalblogapplication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String name;

    private String backendUrl;

    private String backendUrlShort;

    private Moderation moderation = new Moderation();

    @Getter
    @Setter
    public static class Moderation {

        /** Open reports from different users that hide a post until a moderator decides. */
        private int autoHideReports = 3;
    }

}
