package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.ExpertApplication;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import com.project.agriculturalblogapplication.enums.CredentialType;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.ExpertApplicationRequest;
import com.project.agriculturalblogapplication.model.response.ExpertApplicationResponse;
import com.project.agriculturalblogapplication.repositories.ExpertApplicationRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpertServiceTest {

    private static final String LANG = "en";
    private static final long APPLICANT = 7L;

    private final ExpertApplicationRepository repository = mock(ExpertApplicationRepository.class);
    private final UserService userService = mock(UserService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final ImageService imageService = mock(ImageService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ExpertService expertService = new ExpertService(repository, userService, categoryService, imageService,
            authorization);

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(APPLICANT);
        user.setName("Rahim");
        when(authorization.currentUserId(LANG)).thenReturn(APPLICANT);
        when(userService.findByIdWithException(APPLICANT, LANG)).thenReturn(user);
        when(categoryService.findByIdWithException(3L)).thenReturn(category(3L, "Poultry"));
        when(imageService.requireUploaded(any(), any())).thenAnswer(call -> call.getArgument(0));
        when(repository.save(any(ExpertApplication.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void anApplicationWaitsForReviewWithTheCallerAsApplicant() {
        ExpertApplicationResponse response = expertService.apply(request(List.of("/uploads/a.jpg"), null), LANG);

        assertEquals(ApplicationStatus.PENDING, response.status());
        assertEquals(APPLICANT, response.userId());
        assertEquals("Poultry", response.topics().getFirst().categoryName());
        assertEquals("Veterinarian", response.fieldTitle());
        assertEquals(List.of(), response.links());
    }

    @Test
    void aLinkAloneIsEnoughProof() {
        ExpertApplicationResponse response = expertService.apply(request(null, List.of("https://bau.edu.bd/me")), LANG);

        assertEquals(List.of("https://bau.edu.bd/me"), response.links());
    }

    @Test
    void anApplicationWithoutProofIsRefused() {
        ApplicationException e = assertThrows(ApplicationException.class,
                () -> expertService.apply(request(List.of(), List.of()), LANG));

        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void aSecondApplicationWhileOneIsPendingIsAConflict() {
        when(repository.existsByUserIdAndStatus(APPLICANT, ApplicationStatus.PENDING)).thenReturn(true);

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> expertService.apply(request(List.of("/uploads/a.jpg"), null), LANG));

        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void proofImagesMustHaveBeenUploadedHere() {
        when(imageService.requireUploaded(any(), any()))
                .thenThrow(new ApplicationException(HttpStatus.BAD_REQUEST, "not uploaded", LANG));

        assertThrows(ApplicationException.class,
                () -> expertService.apply(request(List.of("https://evil.example/x.jpg"), null), LANG));
        verify(repository, never()).save(any());
    }

    private static ExpertApplicationRequest request(List<String> images, List<String> links) {
        ExpertApplicationRequest request = new ExpertApplicationRequest();
        request.setCategoryIds(List.of(3L, 3L));
        request.setFieldTitle("  Veterinarian ");
        request.setCredentialType(CredentialType.DEGREE);
        request.setInstitution("Bangladesh Agricultural University");
        request.setProofImageUrls(images);
        request.setLinks(links);
        return request;
    }

    private static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setCategoryName(name);
        return category;
    }
}
