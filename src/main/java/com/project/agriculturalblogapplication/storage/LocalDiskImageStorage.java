package com.project.agriculturalblogapplication.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnExpression("'${app.storage.r2.bucket:}'.isEmpty()")
@RequiredArgsConstructor
public class LocalDiskImageStorage implements ImageStorage {

    private final StorageProperties storageProperties;

    @Override
    public String store(byte[] bytes, ImageType type) {
        String fileName = StorageProperties.newFileName(type);
        try {
            Path dir = Path.of(storageProperties.getLocalDir()).toAbsolutePath();
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store image", e);
        }
        return storageProperties.publicUrl(fileName);
    }

    @Override
    public boolean isStored(String url) {
        return storageProperties.isPublicUrl(url);
    }
}
