package com.project.agriculturalblogapplication.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    @Test
    void r2StorageUploadsUnderARandomNameWithItsTypeAndReturnsThePublicUrl() {
        StorageProperties properties = new StorageProperties();
        properties.setPublicBaseUrl("https://images.example.com");
        properties.getR2().setBucket("agrotrends-images");
        S3Client s3 = mock(S3Client.class);

        R2ImageStorage storage = new R2ImageStorage(properties, s3);
        String url = storage.store(WEBP, ImageType.WEBP);

        assertTrue(url.matches("https://images\\.example\\.com/[0-9a-f-]{36}\\.webp"), url);
        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertEquals("agrotrends-images", put.getValue().bucket());
        assertEquals(url.substring(url.lastIndexOf('/') + 1), put.getValue().key());
        assertEquals("image/webp", put.getValue().contentType());
        assertTrue(storage.isStored(url));
        assertFalse(storage.isStored("https://images.example.com.evil.net/a.webp"));
    }

    @Test
    void onlyUrlsThisStorageReturnedCountAsStored(@TempDir Path dir) {
        StorageProperties properties = new StorageProperties();
        properties.setLocalDir(dir.toString());
        properties.setPublicBaseUrl("https://api.example.com/uploads/");
        LocalDiskImageStorage storage = new LocalDiskImageStorage(properties);

        assertTrue(storage.isStored(storage.store(PNG, ImageType.PNG)));
        assertFalse(storage.isStored("https://tracker.example.net/pixel.png"));
        assertFalse(storage.isStored("https://api.example.com/uploads.evil.net/a.png"));
        assertFalse(storage.isStored("https://api.example.com/uploads/../secret/a.png"));
        assertFalse(storage.isStored(null));
    }
}
