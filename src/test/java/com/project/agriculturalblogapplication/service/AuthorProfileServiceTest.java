package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.ExpertBadge;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.UpdateAuthorProfileRequest;
import com.project.agriculturalblogapplication.model.response.AuthorProfileResponse;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private final AnswerService answerService = mock(AnswerService.class);
    private final ExpertService expertService = mock(ExpertService.class);
    private final AuthorProfileService service = new AuthorProfileService(authorService, blogService, followService,
            authorization, answerService, expertService);

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
    void theProfileIsAPortfolioOfExpertTopicsAnswersAndLinks() {
        Author author = author();
        author.setLinks(List.of("https://www.linkedin.com/in/rahim"));
        when(authorService.findByIdWithException(20L)).thenReturn(author);
        Category rice = new Category();
        rice.setId(3L);
        rice.setCategoryName("Rice");
        ExpertBadge badge = new ExpertBadge();
        badge.setTopic(rice);
        badge.setFieldTitle("Agronomist, BRRI");
        when(expertService.activeBadges(10L)).thenReturn(List.of(badge));
        when(answerService.countPublicByUser(10L)).thenReturn(40L);
        when(answerService.countAcceptedByUser(10L)).thenReturn(9L);

        AuthorProfileResponse profile = service.get(20L, "en");

        assertEquals(10L, profile.userId());
        assertEquals(List.of(new AuthorProfileResponse.ExpertTopic(3L, "Rice", "Agronomist, BRRI")), profile.expertTopics());
        assertEquals(40L, profile.answerCount());
        assertEquals(9L, profile.acceptedAnswerCount());
        assertEquals(List.of("https://www.linkedin.com/in/rahim"), profile.links());
    }

    @Test
    void profileLinksMustBeHttpAndAtMostFive() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        UpdateAuthorProfileRequest request = new UpdateAuthorProfileRequest();
        request.setDesignation("Agronomist");
        request.setSpecialities(List.of("rice"));

        request.setLinks(List.of("https://rahim.example.com"));
        assertTrue(validator.validate(request).isEmpty());
        // A javascript: link would run in the visitor's browser from the profile page.
        request.setLinks(List.of("javascript:alert(1)"));
        assertFalse(validator.validate(request).isEmpty());
        request.setLinks(List.of("https://a.com", "https://b.com", "https://c.com", "https://d.com", "https://e.com", "https://f.com"));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void myProfileIsTheCallersOwnForPrefillingTheEditor() {
        Author mine = author();
        when(authorService.profileOf(7L)).thenReturn(mine);

        assertEquals("Rahim", service.getMine("en").name());
        verify(authorService, never()).findByIdWithException(any());
    }

    @Test
    void editingSavesTheBio() {
        Author mine = author();
        when(authorService.profileOf(7L)).thenReturn(mine);
        when(authorService.save(mine)).thenReturn(mine);
        UpdateAuthorProfileRequest request = new UpdateAuthorProfileRequest();
        request.setDesignation("Agronomist");
        request.setSpecialities(List.of("rice"));
        request.setBio("Twenty years in paddy research.");
        request.setLinks(List.of("https://rahim.example.com"));

        AuthorProfileResponse profile = service.updateMine(request, "en");

        assertEquals("Twenty years in paddy research.", profile.bio());
        assertEquals(List.of("https://rahim.example.com"), profile.links());
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
