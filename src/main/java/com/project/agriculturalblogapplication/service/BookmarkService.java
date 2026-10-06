package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Bookmark;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.model.response.BlogResponse;
import com.project.agriculturalblogapplication.repositories.BookmarkRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A reader's private reading list. Every operation acts on the caller's own bookmarks only. */
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    private final BlogService blogService;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    /** Idempotent: saving an already saved blog changes nothing. A blog the caller cannot see is a 404. */
    public void add(Long blogId, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        var blog = blogService.findVisibleBlog(blogId, lang);
        if (bookmarkRepository.existsByUserIdAndBlogId(userId, blogId)) {
            return;
        }
        Bookmark bookmark = new Bookmark();
        bookmark.setUser(userService.findByIdWithException(userId, lang));
        bookmark.setBlog(blog);
        bookmarkRepository.save(bookmark);
    }

    /** Idempotent: removing a blog that is not saved is not an error. */
    public void remove(Long blogId, String lang) {
        bookmarkRepository.deleteByUserIdAndBlogId(authorizationService.currentUserId(lang), blogId);
    }

    /** Saved blogs that are still published, most recently saved first. */
    @Transactional(readOnly = true)
    public Page<BlogResponse> mine(int pageNo, int pageSize, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        return bookmarkRepository.findAllByUserIdAndBlogStatus(userId, BlogStatus.PUBLISHED,
                        CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "creationDate")))
                .map(bookmark -> BlogResponse.from(bookmark.getBlog()));
    }
}
