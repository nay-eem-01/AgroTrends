package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.model.request.UpdateAuthorProfileRequest;
import com.project.agriculturalblogapplication.model.response.AuthorProfileResponse;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Public author pages and an author's edits to their own page. */
@Service
@RequiredArgsConstructor
public class AuthorProfileService {

    private final AuthorService authorService;

    private final BlogService blogService;

    private final FollowService followService;

    private final AuthorizationService authorizationService;

    public AuthorProfileResponse get(Long authorId, String lang) {
        Long callerId = authorizationService.currentUserId(lang);
        return toResponse(authorService.findByIdWithException(authorId), callerId);
    }

    /** Edits the caller's own author profile; 403 for users who are not authors. */
    public AuthorProfileResponse updateMine(UpdateAuthorProfileRequest request, String lang) {
        Long callerId = authorizationService.currentUserId(lang);
        Author author = authorService.findByUserIdOrForbidden(callerId, lang);
        author.setDesignation(request.getDesignation());
        author.setSpecialities(request.getSpecialities());
        author.setOccupation(request.getOccupation());
        author.setWorkPlaceOrInstitution(request.getWorkPlaceOrInstitution());
        author.setBio(request.getBio());
        author.setProfileImageUrl(request.getProfileImageUrl());
        return toResponse(authorService.save(author), callerId);
    }

    private AuthorProfileResponse toResponse(Author author, Long callerId) {
        return new AuthorProfileResponse(author.getId(), author.getUser().getName(), author.getDesignation(),
                author.getOccupation(), author.getWorkPlaceOrInstitution(),
                author.getSpecialities() == null ? List.of() : List.copyOf(author.getSpecialities()),
                author.getBio(), author.getProfileImageUrl(),
                blogService.countPublishedByAuthor(author.getId()), followService.followerCount(author.getId()),
                followService.isFollowingAuthor(callerId, author.getId()));
    }
}
