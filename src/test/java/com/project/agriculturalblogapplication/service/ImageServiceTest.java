package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.storage.ImageStorage;
import com.project.agriculturalblogapplication.storage.ImageType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};

    private final ImageStorage storage = mock(ImageStorage.class);
    private final ImageService imageService = new ImageService(storage);

    @Test
    void aPngIsStored() {
        when(storage.store(PNG, ImageType.PNG)).thenReturn("https://cdn/x.png");

        assertEquals("https://cdn/x.png", imageService.upload(PNG, "en"));
    }

    @Test
    void htmlOrSvgDisguisedAsAnImageIsRejected() {
        assertBadRequest("<svg xmlns='http://www.w3.org/2000/svg' onload='alert(1)'/>".getBytes());
    }

    @Test
    void emptyAndOversizedUploadsAreRejected() {
        assertBadRequest(new byte[0]);
        byte[] huge = new byte[(int) ImageService.MAX_IMAGE_BYTES + 1];
        System.arraycopy(PNG, 0, huge, 0, PNG.length);
        assertBadRequest(huge);
    }

    private void assertBadRequest(byte[] bytes) {
        ApplicationException e = assertThrows(ApplicationException.class, () -> imageService.upload(bytes, "en"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        verify(storage, never()).store(any(), any());
    }
}
