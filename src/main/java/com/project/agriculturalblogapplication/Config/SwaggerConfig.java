package com.project.agriculturalblogapplication.Config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@SecurityScheme(
        name = "jwtToken",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP
)
class SwaggerConfig {

    private final AppProperties appProperties;

    @Bean
    public OpenAPI springShopOpenAPI() {
        String appName = appProperties.getName();

        Info info = new Info();
        info.title(appName + " API");
        info.description(appName + " API Documentation");
        info.version("Version 1.0.0");
        info.license(new License().name("Apache 2.0").url("https://springdoc.org"));

        Server server = new Server();
        server.setUrl(appProperties.getBackendUrl());

        return new OpenAPI()
                .info(info)
                .externalDocs(new ExternalDocumentation())
                .servers(List.of(server));
    }
}