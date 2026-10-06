package com.project.agriculturalblogapplication.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocalDiskImageStorage implements ImageStorage {

    private final StorageProperties storageProperties;

    @Override
    public String store(byte[] bytes, ImageType type) {
        // The name is generated, never taken from the upload, so no path tricks are possible.
        String fileName = UUID.randomUUID() + "." + type.extension();
        try {
            Path dir = Path.of(storageProperties.getLocalDir()).toAbsolutePath();
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store image", e);
        }
        return storageProperties.getPublicBaseUrl().replaceAll("/+$", "") + "/" + fileName;
    }
}
