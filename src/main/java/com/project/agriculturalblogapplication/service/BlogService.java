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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepositories blogRepositories;

    private final CategoryService categoryService;

    private final AuthorService authorService;

    public Blog create(CreateBlogRequest request) {
        Category category = categoryService.findByIdWithException(request.getCategoryId());

        Author author = authorService.findByUserIdWithException(request.getAuthorUserId());

        Blog blog = new Blog();
        blog.setCategory(category);
        blog.setAuthor(author);
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setImageUrl(request.getImageUrl());

        return blogRepositories.save(blog);
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

    public Blog update(UpdateBlogRequest request) {
        Category category = categoryService.findByIdWithException(request.getCategoryId());

        Blog blog = findByIdWithException(request.getBlogId());
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setCategory(category);
        blog.setImageUrl(request.getImageUrl());

        return blogRepositories.save(blog);
    }

    public void delete(Long id) {
        Blog blog = findByIdWithException(id);
        blogRepositories.delete(blog);
    }

    public Blog findByIdWithException(Long blogId) {
        return blogRepositories.findById(blogId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_BLOG_NOT_FOUND));
    }
}
