package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.UpdateAuthorProfileRequest;
import com.project.agriculturalblogapplication.model.request.UpdateUserProfileRequest;
import com.project.agriculturalblogapplication.model.response.UserProfileResponse;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.model.response.AuthorProfileResponse;
import com.project.agriculturalblogapplication.model.response.AuthorProfileResponse.ExpertTopic;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private final QuestionService questionService;

    private final UserService userService;

    private final ImageService imageService;

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

    /** Public: the way from a {@code userId} on a question or answer to the person and their author page. */
    @Transactional(readOnly = true)
    public UserProfileResponse summary(Long userId, String lang) {
        return toSummary(userService.findByIdWithException(userId, lang));
    }

    /** Edits the caller's own location and cover; the cover must be an image uploaded here. */
    public UserProfileResponse updateSummary(UpdateUserProfileRequest request, String lang) {
        User user = userService.findByIdWithException(authorizationService.currentUserId(lang), lang);
        String coverUrl = StringUtils.hasText(request.getCoverUrl())
                ? imageService.requireUploaded(List.of(request.getCoverUrl()), lang).get(0) : null;
        user.setLocation(StringUtils.hasText(request.getLocation()) ? request.getLocation().strip() : null);
        user.setCoverUrl(coverUrl);
        return toSummary(userService.saveUser(user));
    }

    private UserProfileResponse toSummary(User user) {
        Long authorId = authorService.findAuthorIdByUserId(user.getId()).orElse(null);
        return new UserProfileResponse(user.getId(), user.getName(), authorId, user.getLocation(), user.getCoverUrl(),
                CommonUtils.toInstant(user.getCreationDate()),
                answerService.countPublicByUser(user.getId()), answerService.countAcceptedByUser(user.getId()),
                questionService.countPublicByUser(user.getId()),
                authorId == null ? 0 : blogService.countPublishedByAuthor(authorId),
                expertService.checkedCredentials(user.getId()).stream()
                        .map(application -> new UserProfileResponse.Credential(application.getFieldTitle(),
                                application.getInstitution()))
                        .toList());
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
