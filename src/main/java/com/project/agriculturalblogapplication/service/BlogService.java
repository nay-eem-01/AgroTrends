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
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogService {

    private static final int REINDEX_PAGE_SIZE = 50;

    private static final int MAX_RELATED = 10;

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

        blog = blogRepositories.save(blog);
        documentService.indexBlog(blog);

        return BlogResponse.from(blog);
    }

    public Page<BlogResponse> getAll(PaginationArgs paginationArgs) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);
        return blogRepositories.findAll(pageable).map(BlogResponse::from);
    }

    public Page<BlogResponse> getAllByCategory(PaginationArgs paginationArgs, Long categoryId) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);

        Category category = categoryService.findByIdWithException(categoryId);
        List<Blog> blogs = blogRepositories.findAllByCategory((category));

        return new PageImpl<>(blogs.stream().map(BlogResponse::from).toList(), pageable, blogs.size());
    }


    public Page<BlogResponse> getAllByAuthor(PaginationArgs paginationArgs, Long authorUserId) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);

        Author author = authorService.findByUserIdWithException(authorUserId);
        List<Blog> blogs = blogRepositories.findAllByAuthor(author);

        return new PageImpl<>(blogs.stream().map(BlogResponse::from).toList(), pageable, blogs.size());
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
        documentService.reindexBlog(blog);

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
            page.forEach(documentService::reindexBlog);
            count += page.getNumberOfElements();
        } while (page.hasNext());
        return count;
    }

    /** Up to {@code limit} (1..10) other posts on similar topics, most similar first. */
    public List<RelatedBlogResponse> related(Long blogId, int limit) {
        Blog blog = findByIdWithException(blogId);
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

    public BlogResponse getById(Long blogId) {
        return BlogResponse.from(findByIdWithException(blogId));
    }

    public Blog findByIdWithException(Long blogId) {
        return blogRepositories.findById(blogId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_BLOG_NOT_FOUND));
    }
}
