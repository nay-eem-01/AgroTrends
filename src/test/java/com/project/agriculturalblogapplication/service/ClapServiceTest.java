package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Clap;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.ClapResponse;
import com.project.agriculturalblogapplication.repositories.ClapRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClapServiceTest {

    private static final String LANG = "en";
    private static final long READER = 7L;
    private static final long AUTHOR = 10L;

    private final ClapRepository clapRepository = mock(ClapRepository.class);
    private final BlogService blogService = mock(BlogService.class);
    private final UserService userService = mock(UserService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ClapService clapService = new ClapService(clapRepository, blogService, userService, authorization);

    private Blog blog;

    @BeforeEach
    void setUp() {
        User author = new User();
        author.setId(AUTHOR);
        Author profile = new Author();
        profile.setUser(author);
        blog = new Blog();
        blog.setId(1L);
        blog.setAuthor(profile);
        blog.setClapCount(30);
        when(blogService.findByIdWithException(1L)).thenReturn(blog);
        when(authorization.currentUserId(LANG)).thenReturn(READER);
    }

    @Test
    void aFirstClapCreatesTheRowAndRaisesTheTotal() {
        when(clapRepository.findByBlogIdAndUserId(1L, READER)).thenReturn(Optional.empty());

        assertEquals(new ClapResponse(35, 5), clapService.clap(1L, 5, LANG));

        verify(clapRepository).save(any(Clap.class));
        verify(blogService).addClaps(1L, 5);
    }

    @Test
    void clapsStopAtFiftyPerReader() {
        when(clapRepository.findByBlogIdAndUserId(1L, READER)).thenReturn(Optional.of(clap(48)));

        assertEquals(new ClapResponse(32, 50), clapService.clap(1L, 10, LANG));
        verify(blogService).addClaps(1L, 2);
    }

    @Test
    void aReaderAtTheCapChangesNothing() {
        when(clapRepository.findByBlogIdAndUserId(1L, READER)).thenReturn(Optional.of(clap(50)));

        assertEquals(new ClapResponse(30, 50), clapService.clap(1L, 1, LANG));
        verify(blogService, never()).addClaps(anyLong(), anyLong());
    }

    @Test
    void authorsCannotClapForThemselves() {
        when(authorization.currentUserId(LANG)).thenReturn(AUTHOR);

        assertStatus(HttpStatus.FORBIDDEN, () -> clapService.clap(1L, 1, LANG));
    }

    @Test
    void draftsCannotBeClappedAndCountsMustBeOneToFifty() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> clapService.clap(1L, 0, LANG));
        assertStatus(HttpStatus.BAD_REQUEST, () -> clapService.clap(1L, 51, LANG));
        blog.setStatus(BlogStatus.DRAFT);
        assertStatus(HttpStatus.NOT_FOUND, () -> clapService.clap(1L, 1, LANG));
    }

    @Test
    void takingClapsBackLowersTheTotal() {
        Clap mine = clap(12);
        when(clapRepository.findByBlogIdAndUserId(1L, READER)).thenReturn(Optional.of(mine));

        assertEquals(new ClapResponse(18, 0), clapService.removeMyClaps(1L, LANG));
        verify(clapRepository).delete(mine);
        verify(blogService).addClaps(1L, -12);
    }

    private static Clap clap(int count) {
        Clap clap = new Clap();
        clap.setCount(count);
        return clap;
    }

    private static void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable call) {
        assertEquals(status, assertThrows(ApplicationException.class, call).getHttpStatus());
    }
}
