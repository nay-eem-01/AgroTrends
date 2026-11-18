package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Clap;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.ClapResponse;
import com.project.agriculturalblogapplication.repositories.ClapRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Medium-style claps: a reader may clap a published post up to {@value #MAX_CLAPS_PER_USER} times. */
@Service
@RequiredArgsConstructor
public class ClapService {

    public static final int MAX_CLAPS_PER_USER = 50;

    private final ClapRepository clapRepository;

    private final BlogService blogService;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    /** Adds {@code count} (1..50) claps; anything over the per-user cap is ignored rather than refused. */
    @Transactional
    public ClapResponse clap(Long blogId, int count, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        Blog blog = publishedBlog(blogId, lang);
        if (blog.getAuthor().getUser().getId().equals(userId)) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_CANNOT_CLAP_OWN_BLOG, lang);
        }
        if (count < 1 || count > MAX_CLAPS_PER_USER) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_CLAP_COUNT, lang);
        }
        Clap clap = clapRepository.findByBlogIdAndUserId(blogId, userId).orElseGet(() -> newClap(blog, userId, lang));
        int added = Math.min(count, MAX_CLAPS_PER_USER - clap.getCount());
        if (added > 0) {
            clap.setCount(clap.getCount() + added);
            clapRepository.save(clap);
            blogService.addClaps(blogId, added);
        }
        return new ClapResponse(blog.getClapCount() + added, clap.getCount());
    }

    /** Takes back all of the caller's claps on a post. */
    @Transactional
    public ClapResponse removeMyClaps(Long blogId, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        Blog blog = publishedBlog(blogId, lang);
        int removed = clapRepository.findByBlogIdAndUserId(blogId, userId).map(clap -> {
            clapRepository.delete(clap);
            blogService.addClaps(blogId, -clap.getCount());
            return clap.getCount();
        }).orElse(0);
        return new ClapResponse(blog.getClapCount() - removed, 0);
    }

    public ClapResponse get(Long blogId, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        Blog blog = publishedBlog(blogId, lang);
        int mine = clapRepository.findByBlogIdAndUserId(blogId, userId).map(Clap::getCount).orElse(0);
        return new ClapResponse(blog.getClapCount(), mine);
    }

    /** Claps are for published posts; a draft answers 404 like everywhere else. */
    private Blog publishedBlog(Long blogId, String lang) {
        Blog blog = blogService.findByIdWithException(blogId);
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_BLOG_NOT_FOUND, lang);
        }
        return blog;
    }

    private Clap newClap(Blog blog, Long userId, String lang) {
        Clap clap = new Clap();
        clap.setBlog(blog);
        clap.setUser(userService.findByIdWithException(userId, lang));
        return clap;
    }
}
