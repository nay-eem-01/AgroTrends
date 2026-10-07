package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateBlogRequest;
import com.project.agriculturalblogapplication.model.request.UpdateBlogRequest;
import com.project.agriculturalblogapplication.model.response.BlogResponse;
import com.project.agriculturalblogapplication.model.response.RelatedBlogResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogService {

    private static final int REINDEX_PAGE_SIZE = 50;

    private static final int MAX_RELATED = 10;

    static final Set<String> SORTABLE_FIELDS = Set.of("creationDate", "lastModifiedDate", "title");

    private final BlogRepositories blogRepositories;

    private final CategoryService categoryService;

    private final AuthorService authorService;

    private final DocumentService documentService;

    private final AuthorizationService authorizationService;

    public BlogResponse create(CreateBlogRequest request, String lang) {
        Category category = categoryService.findByIdWithException(request.getCategoryId());

        Author author = authorService.findByUserIdOrForbidden(authorizationService.currentUserId(lang), lang);

        Blog blog = new Blog();
        blog.setCategory(category);
        blog.setAuthor(author);
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setImageUrl(request.getImageUrl());
        blog.setStatus(request.getStatus() == null ? BlogStatus.PUBLISHED : request.getStatus());
        if (blog.getStatus() == BlogStatus.PUBLISHED) {
            blog.setPublishedAt(LocalDateTime.now());
        }

        blog = blogRepositories.save(blog);
        if (blog.getStatus() == BlogStatus.PUBLISHED) {
            documentService.indexBlog(blog);
        }

        return BlogResponse.from(blog);
    }

    public Page<BlogResponse> getAll(PaginationArgs paginationArgs, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs, SORTABLE_FIELDS, lang);
        return blogRepositories.findAllByStatus(BlogStatus.PUBLISHED, pageable).map(BlogResponse::from);
    }

    public Page<BlogResponse> getAllByCategory(PaginationArgs paginationArgs, Long categoryId, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs, SORTABLE_FIELDS, lang);
        Category category = categoryService.findByIdWithException(categoryId);
        return blogRepositories.findAllByCategoryAndStatus(category, BlogStatus.PUBLISHED, pageable).map(BlogResponse::from);
    }

    /** {@code authorId} is the Author id, the same id a blog response shows as {@code author.authorId}. */
    public Page<BlogResponse> getAllByAuthor(PaginationArgs paginationArgs, Long authorId, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs, SORTABLE_FIELDS, lang);
        Author author = authorService.findByIdWithException(authorId);
        return blogRepositories.findAllByAuthorAndStatus(author, BlogStatus.PUBLISHED, pageable).map(BlogResponse::from);
    }

    /** The caller's own drafts; 403 for users without an author profile. */
    public Page<BlogResponse> getMyDrafts(PaginationArgs paginationArgs, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs, SORTABLE_FIELDS, lang);
        Author author = authorService.findByUserIdOrForbidden(authorizationService.currentUserId(lang), lang);
        return blogRepositories.findAllByAuthorAndStatus(author, BlogStatus.DRAFT, pageable).map(BlogResponse::from);
    }

    public BlogResponse publish(Long blogId, String lang) {
        Blog blog = findByIdWithException(blogId);
        authorizationService.assertOwnerOrAdmin(blog.getAuthor().getUser().getId(), lang);
        if (blog.getStatus() == BlogStatus.PUBLISHED) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_BLOG_ALREADY_PUBLISHED, lang);
        }
        blog.setStatus(BlogStatus.PUBLISHED);
        if (blog.getPublishedAt() == null) {
            blog.setPublishedAt(LocalDateTime.now());
        }
        blog = blogRepositories.save(blog);
        documentService.indexBlog(blog);
        return BlogResponse.from(blog);
    }

    /** Back to draft: the post leaves lists and its chunks leave the vector store, so AI answers stop citing it. */
    public BlogResponse unpublish(Long blogId, String lang) {
        Blog blog = findByIdWithException(blogId);
        authorizationService.assertOwnerOrAdmin(blog.getAuthor().getUser().getId(), lang);
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_BLOG_NOT_PUBLISHED, lang);
        }
        blog.setStatus(BlogStatus.DRAFT);
        blog = blogRepositories.save(blog);
        documentService.deleteBlog(blog.getId());
        return BlogResponse.from(blog);
    }

    public BlogResponse update(UpdateBlogRequest request, String lang) {
        Blog blog = findByIdWithException(request.getBlogId());
        authorizationService.assertOwnerOrAdmin(blog.getAuthor().getUser().getId(), lang);

        Category category = categoryService.findByIdWithException(request.getCategoryId());
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setCategory(category);
        blog.setImageUrl(request.getImageUrl());

        blog = blogRepositories.save(blog);
        if (blog.getStatus() == BlogStatus.PUBLISHED) {
            documentService.reindexBlog(blog);
        }

        return BlogResponse.from(blog);
    }

    public void delete(Long id, String lang) {
        Blog blog = findByIdWithException(id);
        authorizationService.assertOwnerOrAdmin(blog.getAuthor().getUser().getId(), lang);
        blogRepositories.delete(blog);
        documentService.deleteBlog(blog.getId());
    }

    /** Re-embeds every blog, page by page. Used after an embedding model change; returns the number of blogs. */
    public long reindexAll() {
        long count = 0;
        Page<Blog> page;
        int pageNo = 0;
        do {
            page = blogRepositories.findAll(PageRequest.of(pageNo++, REINDEX_PAGE_SIZE, Sort.by("id")));
            page.forEach(blog -> {
                if (blog.getStatus() == BlogStatus.PUBLISHED) {
                    documentService.reindexBlog(blog);
                } else {
                    documentService.deleteBlog(blog.getId());
                }
            });
            count += page.getNumberOfElements();
        } while (page.hasNext());
        return count;
    }

    /** Up to {@code limit} (1..10) other posts on similar topics, most similar first. */
    public List<RelatedBlogResponse> related(Long blogId, int limit, String lang) {
        Blog blog = findVisibleBlog(blogId, lang);
        List<Long> ids = documentService.findRelatedBlogIds(blog, Math.min(Math.max(limit, 1), MAX_RELATED));
        Map<Long, Blog> blogs = blogRepositories.findAllById(ids).stream()
                .collect(Collectors.toMap(Blog::getId, Function.identity()));
        // Keep the similarity order; skip ids whose blog is gone (vectors are removed on delete, but be safe).
        return ids.stream()
                .map(blogs::get)
                .filter(Objects::nonNull)
                .map(related -> new RelatedBlogResponse(related.getId(), related.getTitle()))
                .toList();
    }

    public BlogResponse getById(Long blogId, String lang) {
        return BlogResponse.from(findVisibleBlog(blogId, lang));
    }

    /** Published posts for everyone; a draft only for its author and admins, and a 404 for anyone else. */
    private Blog findVisibleBlog(Long blogId, String lang) {
        Blog blog = findByIdWithException(blogId);
        if (blog.getStatus() != BlogStatus.PUBLISHED
                && !authorizationService.isOwnerOrAdmin(blog.getAuthor().getUser().getId(), lang)) {
            throw new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_BLOG_NOT_FOUND, lang);
        }
        return blog;
    }

    public Blog findByIdWithException(Long blogId) {
        return blogRepositories.findById(blogId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_BLOG_NOT_FOUND));
    }
}
