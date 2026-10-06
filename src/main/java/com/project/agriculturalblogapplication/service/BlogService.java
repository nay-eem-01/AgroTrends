package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateBlogRequest;
import com.project.agriculturalblogapplication.model.request.UpdateBlogRequest;
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

@Service
@RequiredArgsConstructor
public class BlogService {

    private static final int REINDEX_PAGE_SIZE = 50;

    private final BlogRepositories blogRepositories;

    private final CategoryService categoryService;

    private final AuthorService authorService;

    private final DocumentService documentService;

    private final AuthorizationService authorizationService;

    public Blog create(CreateBlogRequest request, String lang) {
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

        return blog;
    }

    public Page<Blog> getAll(PaginationArgs paginationArgs) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);
        return blogRepositories.findAll(pageable);
    }

    public Page<Blog> getAllByCategory(PaginationArgs paginationArgs, Long categoryId) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);

        Category category = categoryService.findByIdWithException(categoryId);
        List<Blog> blogs = blogRepositories.findAllByCategory((category));

        return new PageImpl<>(blogs, pageable, blogs.size());
    }


    public Page<Blog> getAllByAuthor(PaginationArgs paginationArgs, Long authorUserId) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);

        Author author = authorService.findByUserIdWithException(authorUserId);
        List<Blog> blogs = blogRepositories.findAllByAuthor(author);

        return new PageImpl<>(blogs, pageable, blogs.size());
    }

    public Blog update(UpdateBlogRequest request, String lang) {
        Blog blog = findByIdWithException(request.getBlogId());
        authorizationService.assertOwnerOrAdmin(blog.getAuthor().getUser().getId(), lang);

        Category category = categoryService.findByIdWithException(request.getCategoryId());
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setCategory(category);
        blog.setImageUrl(request.getImageUrl());

        blog = blogRepositories.save(blog);
        documentService.reindexBlog(blog);

        return blog;
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

    public Blog findByIdWithException(Long blogId) {
        return blogRepositories.findById(blogId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_BLOG_NOT_FOUND));
    }
}
