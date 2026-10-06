package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Bookmark;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.BookmarkRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookmarkServiceTest {

    private final BookmarkRepository bookmarkRepository = mock(BookmarkRepository.class);
    private final BlogService blogService = mock(BlogService.class);
    private final UserService userService = mock(UserService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final BookmarkService bookmarkService = new BookmarkService(bookmarkRepository, blogService, userService, authorization);

    @BeforeEach
    void setUp() {
        when(authorization.currentUserId("en")).thenReturn(7L);
    }

    @Test
    void savingTwiceStoresOneBookmark() {
        when(blogService.findVisibleBlog(1L, "en")).thenReturn(new Blog());
        when(bookmarkRepository.existsByUserIdAndBlogId(7L, 1L)).thenReturn(false, true);

        bookmarkService.add(1L, "en");
        bookmarkService.add(1L, "en");

        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    void someoneElsesDraftCannotBeSaved() {
        when(blogService.findVisibleBlog(1L, "en")).thenThrow(new ApplicationException(HttpStatus.NOT_FOUND, "not found"));

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApplicationException.class, () -> bookmarkService.add(1L, "en")).getHttpStatus());
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void removeAndListOnlyTouchTheCallersOwnBookmarks() {
        when(bookmarkRepository.findAllByUserIdAndBlogStatus(eq(7L), eq(BlogStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(Page.empty());

        bookmarkService.remove(1L, "en");
        bookmarkService.mine(0, 20, "en");

        verify(bookmarkRepository).deleteByUserIdAndBlogId(7L, 1L);
        verify(bookmarkRepository).findAllByUserIdAndBlogStatus(eq(7L), eq(BlogStatus.PUBLISHED), any(Pageable.class));
    }
}
