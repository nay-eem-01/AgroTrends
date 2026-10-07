package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.AuthorFollow;
import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.entities.TagFollow;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.AuthorSummaryResponse;
import com.project.agriculturalblogapplication.repositories.AuthorFollowRepository;
import com.project.agriculturalblogapplication.repositories.TagFollowRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Following authors and topics. Every operation acts for the caller; following is idempotent. */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final AuthorFollowRepository authorFollowRepository;

    private final TagFollowRepository tagFollowRepository;

    private final AuthorService authorService;

    private final TagService tagService;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    public void followAuthor(Long authorId, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        Author author = authorService.findByIdWithException(authorId);
        if (author.getUser().getId().equals(userId)) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_CANNOT_FOLLOW_YOURSELF, lang);
        }
        if (!authorFollowRepository.existsByUserIdAndAuthorId(userId, authorId)) {
            AuthorFollow follow = new AuthorFollow();
            follow.setUser(userService.findByIdWithException(userId, lang));
            follow.setAuthor(author);
            authorFollowRepository.save(follow);
        }
    }

    public void unfollowAuthor(Long authorId, String lang) {
        authorFollowRepository.deleteByUserIdAndAuthorId(authorizationService.currentUserId(lang), authorId);
    }

    public void followTag(String tagName, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        Tag tag = tagService.findByNameWithException(tagName, lang);
        if (!tagFollowRepository.existsByUserIdAndTagId(userId, tag.getId())) {
            TagFollow follow = new TagFollow();
            follow.setUser(userService.findByIdWithException(userId, lang));
            follow.setTag(tag);
            tagFollowRepository.save(follow);
        }
    }

    public void unfollowTag(String tagName, String lang) {
        Tag tag = tagService.findByNameWithException(tagName, lang);
        tagFollowRepository.deleteByUserIdAndTagId(authorizationService.currentUserId(lang), tag.getId());
    }

    @Transactional(readOnly = true)
    public Page<AuthorSummaryResponse> followedAuthors(int pageNo, int pageSize, String lang) {
        return authorFollowRepository.findAllByUserId(authorizationService.currentUserId(lang), newestFirst(pageNo, pageSize))
                .map(follow -> AuthorSummaryResponse.from(follow.getAuthor()));
    }

    @Transactional(readOnly = true)
    public Page<String> followedTags(int pageNo, int pageSize, String lang) {
        return tagFollowRepository.findAllByUserId(authorizationService.currentUserId(lang), newestFirst(pageNo, pageSize))
                .map(follow -> follow.getTag().getName());
    }

    public long followerCount(Long authorId) {
        return authorFollowRepository.countByAuthorId(authorId);
    }

    private static Pageable newestFirst(int pageNo, int pageSize) {
        return CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "creationDate"));
    }
}
