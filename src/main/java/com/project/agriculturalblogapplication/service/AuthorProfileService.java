package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.model.request.UpdateAuthorProfileRequest;
import com.project.agriculturalblogapplication.model.response.AuthorProfileResponse;
import com.project.agriculturalblogapplication.model.response.AuthorProfileResponse.ExpertTopic;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Public author pages and an author's edits to their own page. */
@Service
@RequiredArgsConstructor
public class AuthorProfileService {

    private final AuthorService authorService;

    private final BlogService blogService;

    private final FollowService followService;

    private final AuthorizationService authorizationService;

    private final AnswerService answerService;

    private final ExpertService expertService;

    /** Public; {@code followedByMe} is false for anonymous visitors. */
    @Transactional(readOnly = true)
    public AuthorProfileResponse get(Long authorId, String lang) {
        Long callerId = authorizationService.signedInUserId().orElse(null);
        return toResponse(authorService.findByIdWithException(authorId), callerId);
    }

    /** The caller's own profile, to prefill the editor. */
    public AuthorProfileResponse getMine(String lang) {
        Long callerId = authorizationService.currentUserId(lang);
        return toResponse(authorService.profileOf(callerId), callerId);
    }

    /** Edits the caller's own profile. */
    public AuthorProfileResponse updateMine(UpdateAuthorProfileRequest request, String lang) {
        Long callerId = authorizationService.currentUserId(lang);
        Author author = authorService.profileOf(callerId);
        author.setDesignation(request.getDesignation());
        author.setSpecialities(request.getSpecialities());
        author.setOccupation(request.getOccupation());
        author.setWorkPlaceOrInstitution(request.getWorkPlaceOrInstitution());
        author.setBio(request.getBio());
        author.setProfileImageUrl(request.getProfileImageUrl());
        author.setLinks(request.getLinks());
        return toResponse(authorService.save(author), callerId);
    }

    private AuthorProfileResponse toResponse(Author author, Long callerId) {
        Long userId = author.getUser().getId();
        return new AuthorProfileResponse(author.getId(), userId, author.getUser().getName(), author.getDesignation(),
                author.getOccupation(), author.getWorkPlaceOrInstitution(),
                author.getSpecialities() == null ? List.of() : List.copyOf(author.getSpecialities()),
                author.getBio(), author.getProfileImageUrl(),
                author.getLinks() == null ? List.of() : List.copyOf(author.getLinks()),
                expertService.activeBadges(userId).stream()
                        .map(badge -> new ExpertTopic(badge.getTopic().getId(), badge.getTopic().getCategoryName(),
                                badge.getFieldTitle()))
                        .toList(),
                blogService.countPublishedByAuthor(author.getId()), answerService.countPublicByUser(userId),
                answerService.countAcceptedByUser(userId), followService.followerCount(author.getId()),
                callerId != null && followService.isFollowingAuthor(callerId, author.getId()));
    }
}
