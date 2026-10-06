package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.storage.ImageStorage;
import com.project.agriculturalblogapplication.storage.ImageType;
import org.junit.jupiter.api.BeforeEach;
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
    private final AuthorService authorService = mock(AuthorService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ImageService imageService = new ImageService(storage, authorService, authorization);

    @BeforeEach
    void setUp() {
        when(authorization.currentUserId("en")).thenReturn(7L);
    }

    @Test
    void anAuthorsPngIsStored() {
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

    @Test
    void nonAuthorsCannotUpload() {
        when(authorService.findByUserIdOrForbidden(7L, "en")).thenThrow(new ApplicationException(HttpStatus.FORBIDDEN, "forbidden"));

        assertThrows(ApplicationException.class, () -> imageService.upload(PNG, "en"));
        verify(storage, never()).store(any(), any());
    }

    private void assertBadRequest(byte[] bytes) {
        ApplicationException e = assertThrows(ApplicationException.class, () -> imageService.upload(bytes, "en"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        verify(storage, never()).store(any(), any());
    }
}
