package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.AuthorFollow;
import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.entities.TagFollow;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.AuthorFollowRepository;
import com.project.agriculturalblogapplication.repositories.TagFollowRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
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

class FollowServiceTest {

    private final AuthorFollowRepository authorFollows = mock(AuthorFollowRepository.class);
    private final TagFollowRepository tagFollows = mock(TagFollowRepository.class);
    private final AuthorService authorService = mock(AuthorService.class);
    private final TagService tagService = mock(TagService.class);
    private final UserService userService = mock(UserService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final FollowService followService =
            new FollowService(authorFollows, tagFollows, authorService, tagService, userService, authorization);

    @BeforeEach
    void setUp() {
        when(authorization.currentUserId("en")).thenReturn(7L);
    }

    @Test
    void followingAnAuthorTwiceStoresOneFollow() {
        when(authorService.findByIdWithException(20L)).thenReturn(author(10L));
        when(authorFollows.existsByUserIdAndAuthorId(7L, 20L)).thenReturn(false, true);

        followService.followAuthor(20L, "en");
        followService.followAuthor(20L, "en");

        verify(authorFollows).save(any(AuthorFollow.class));
    }

    @Test
    void youCannotFollowYourself() {
        when(authorService.findByIdWithException(20L)).thenReturn(author(7L));

        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApplicationException.class, () -> followService.followAuthor(20L, "en")).getHttpStatus());
        verify(authorFollows, never()).save(any());
    }

    @Test
    void tagsAreFollowedByNameAndUnfollowingIsScopedToTheCaller() {
        Tag rice = new Tag();
        rice.setId(5L);
        rice.setName("rice");
        when(tagService.findByNameWithException("Rice", "en")).thenReturn(rice);

        followService.followTag("Rice", "en");
        followService.unfollowTag("Rice", "en");
        followService.unfollowAuthor(20L, "en");

        verify(tagFollows).save(any(TagFollow.class));
        verify(tagFollows).deleteByUserIdAndTagId(7L, 5L);
        verify(authorFollows).deleteByUserIdAndAuthorId(7L, 20L);
    }

    private static Author author(Long userId) {
        User user = new User();
        user.setId(userId);
        Author author = new Author();
        author.setId(20L);
        author.setUser(user);
        return author;
    }
}
