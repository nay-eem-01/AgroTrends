package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.UpdateAuthorProfileRequest;
import com.project.agriculturalblogapplication.model.response.AuthorProfileResponse;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorProfileServiceTest {

    private final AuthorService authorService = mock(AuthorService.class);
    private final BlogService blogService = mock(BlogService.class);
    private final FollowService followService = mock(FollowService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final AuthorProfileService service = new AuthorProfileService(authorService, blogService, followService, authorization);

    @BeforeEach
    void setUp() {
        when(authorization.currentUserId("en")).thenReturn(7L);
        when(authorization.signedInUserId()).thenReturn(Optional.of(7L));
    }

    @Test
    void anonymousVisitorsSeeTheProfileButFollowNobody() {
        when(authorization.signedInUserId()).thenReturn(Optional.empty());
        when(authorService.findByIdWithException(20L)).thenReturn(author());

        AuthorProfileResponse profile = service.get(20L, "en");

        assertEquals("Rahim", profile.name());
        assertFalse(profile.followedByMe());
        verify(followService, never()).isFollowingAuthor(any(), any());
        verify(authorization, never()).currentUserId(any());
    }

    @Test
    void profileShowsCountsAndWhetherYouFollowWithoutContactDetails() {
        when(authorService.findByIdWithException(20L)).thenReturn(author());
        when(blogService.countPublishedByAuthor(20L)).thenReturn(12L);
        when(followService.followerCount(20L)).thenReturn(340L);
        when(followService.isFollowingAuthor(7L, 20L)).thenReturn(true);

        AuthorProfileResponse profile = service.get(20L, "en");

        assertEquals("Rahim", profile.name());
        assertEquals(List.of("rice", "soil health"), profile.specialities());
        assertEquals(12L, profile.publishedPostCount());
        assertEquals(340L, profile.followerCount());
        assertTrue(profile.followedByMe());
        assertTrue(Arrays.stream(AuthorProfileResponse.class.getRecordComponents())
                .noneMatch(c -> c.getName().matches("(?i).*(email|mobile|role|password).*")));
    }

    @Test
    void myProfileIsTheCallersOwnForPrefillingTheEditor() {
        Author mine = author();
        when(authorService.findByUserIdOrForbidden(7L, "en")).thenReturn(mine);

        assertEquals("Rahim", service.getMine("en").name());
        verify(authorService, never()).findByIdWithException(any());
    }

    @Test
    void readersWithoutAnAuthorProfileGet403ForMyProfile() {
        when(authorService.findByUserIdOrForbidden(7L, "en")).thenThrow(new ApplicationException(HttpStatus.FORBIDDEN, "forbidden"));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApplicationException.class, () -> service.getMine("en")).getHttpStatus());
    }

    @Test
    void onlyAuthorsCanEditAndOnlyTheirOwnProfile() {
        when(authorService.findByUserIdOrForbidden(7L, "en")).thenThrow(new ApplicationException(HttpStatus.FORBIDDEN, "forbidden"));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApplicationException.class,
                () -> service.updateMine(new UpdateAuthorProfileRequest(), "en")).getHttpStatus());
        verify(authorService, never()).save(any());
    }

    @Test
    void editingSavesTheBio() {
        Author mine = author();
        when(authorService.findByUserIdOrForbidden(7L, "en")).thenReturn(mine);
        when(authorService.save(mine)).thenReturn(mine);
        UpdateAuthorProfileRequest request = new UpdateAuthorProfileRequest();
        request.setDesignation("Agronomist");
        request.setSpecialities(List.of("rice"));
        request.setBio("Twenty years in paddy research.");

        assertEquals("Twenty years in paddy research.", service.updateMine(request, "en").bio());
    }

    private static Author author() {
        User user = new User();
        user.setId(10L);
        user.setName("Rahim");
        user.setEmail("rahim@example.com");
        Author author = new Author();
        author.setId(20L);
        author.setUser(user);
        author.setSpecialities(List.of("rice", "soil health"));
        return author;
    }
}
