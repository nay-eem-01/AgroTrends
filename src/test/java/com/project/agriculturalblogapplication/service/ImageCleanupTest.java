package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.storage.ImageStorage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageCleanupTest {
    private final ImageStorage storage = mock(ImageStorage.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ImageCleanup cleanup = new ImageCleanup(storage, events, jdbc);
    private final String url = "https://images.example.com/abc.png";

    @Test
    void collectsOwnedUrlsAndInlineImagesWithoutDeletingBeforeCommit() {
        when(storage.isStored(url)).thenReturn(true);
        cleanup.consider(null, url, "<p>Photo</p><img src='" + url + "'>", "https://other.example.com/a.png");
        ArgumentCaptor<ImageCleanup.DetachedImages> event = ArgumentCaptor.forClass(ImageCleanup.DetachedImages.class);
        verify(events).publishEvent(event.capture());
        assertEquals(Set.of(url), event.getValue().urls());
        verify(storage, never()).delete(any());
    }

    @Test
    void deletesAnUnreferencedObjectAfterCommit() {
        when(storage.isStored(url)).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(url))).thenReturn(false);
        cleanup.afterCommit(new ImageCleanup.DetachedImages(Set.of(url)));
        verify(storage).delete(url);
    }

    @Test
    void preservesSharedImagesAndIgnoresExternalUrls() {
        when(storage.isStored(url)).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(url))).thenReturn(true);
        cleanup.afterCommit(new ImageCleanup.DetachedImages(Set.of(url, "https://external.example/file.png")));
        verify(storage, never()).delete(any());
    }

    @Test
    void databaseFailureNeverDeletesTheObjectOrFailsAnAlreadySavedEdit() {
        when(storage.isStored(url)).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(url))).thenThrow(new IllegalStateException("offline"));
        assertDoesNotThrow(() -> cleanup.afterCommit(new ImageCleanup.DetachedImages(Set.of(url))));
        verify(storage, never()).delete(any());
    }
}
