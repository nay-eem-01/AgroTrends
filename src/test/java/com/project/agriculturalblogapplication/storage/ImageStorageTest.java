package com.project.agriculturalblogapplication.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageStorageTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0};

    @Test
    void typesAreRecognisedByTheirFirstBytes() {
        assertEquals(Optional.of(ImageType.PNG), ImageType.detect(PNG));
        assertEquals(Optional.of(ImageType.JPEG), ImageType.detect(JPEG));
        assertEquals(Optional.of(ImageType.WEBP), ImageType.detect(WEBP));
        assertEquals(Optional.empty(), ImageType.detect("<svg onload=alert(1)>".getBytes()));
        assertEquals(Optional.empty(), ImageType.detect(new byte[]{(byte) 0xFF}));
    }

    @Test
    void localStorageWritesUnderARandomNameAndReturnsThePublicUrl(@TempDir Path dir) throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setLocalDir(dir.toString());
        properties.setPublicBaseUrl("https://api.example.com/uploads/");

        String url = new LocalDiskImageStorage(properties).store(PNG, ImageType.PNG);

        assertTrue(url.matches("https://api\\.example\\.com/uploads/[0-9a-f-]{36}\\.png"), url);
        Path stored = dir.resolve(url.substring(url.lastIndexOf('/') + 1));
        assertArrayEquals(PNG, Files.readAllBytes(stored));
    }
}
