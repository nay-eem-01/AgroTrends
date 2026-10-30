package com.project.agriculturalblogapplication.storage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ImageStorageSelectionTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(StorageProperties.class, LocalDiskImageStorage.class, R2ImageStorage.class);

    @Test
    void withoutABucketImagesStayOnTheLocalDisk() {
        context.run(app -> assertThat(app).getBean(ImageStorage.class).isInstanceOf(LocalDiskImageStorage.class));
    }

    @Test
    void aBucketSwitchesToR2() {
        // Regression: R2ImageStorage had two constructors and Spring could not create it ("No default constructor").
        context.withPropertyValues("app.storage.r2.bucket=images", "app.storage.r2.endpoint=https://example.invalid",
                        "app.storage.r2.access-key-id=x", "app.storage.r2.secret-access-key=y")
                .run(app -> assertThat(app).getBean(ImageStorage.class).isInstanceOf(R2ImageStorage.class));
    }
}
