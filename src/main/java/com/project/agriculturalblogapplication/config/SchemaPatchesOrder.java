package com.project.agriculturalblogapplication.config;

import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Configuration;

/** Makes Hibernate's EntityManagerFactory wait for {@link SchemaPatches}, the way Spring Boot orders Flyway. */
@Configuration
public class SchemaPatchesOrder extends EntityManagerFactoryDependsOnPostProcessor {

    public SchemaPatchesOrder() {
        super(SchemaPatches.class);
    }
}
