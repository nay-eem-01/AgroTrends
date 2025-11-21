package com.project.agriculturalblogapplication.ServiceImplementation;

import com.project.agriculturalblogapplication.DTOS.BlogDto;
import com.project.agriculturalblogapplication.ExceptionHandler.APIExceptionHandler;
import com.project.agriculturalblogapplication.ExceptionHandler.ResourceNotFoundException;
import com.project.agriculturalblogapplication.Models.Blogs;
import com.project.agriculturalblogapplication.Models.Categories;
import com.project.agriculturalblogapplication.Models.Users;
import com.project.agriculturalblogapplication.Repositories.BlogRepositories;
import com.project.agriculturalblogapplication.Repositories.CategoryRepositories;
import com.project.agriculturalblogapplication.Repositories.UserRepositories;
import com.project.agriculturalblogapplication.Service.BlogService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlogServiceImpl implements BlogService {


    private final BlogRepositories blogRepositories;
    private final CategoryRepositories categoryRepositories;
    private final UserRepositories userRepositories;
    private final ModelMapper modelMapper;

    public BlogServiceImpl(BlogRepositories blogRepositories, CategoryRepositories categoryRepositories, UserRepositories userRepositories, ModelMapper modelMapper) {
        this.blogRepositories = blogRepositories;
        this.categoryRepositories = categoryRepositories;
        this.userRepositories = userRepositories;
        this.modelMapper = modelMapper;
    }


    @Override
    public BlogDto addNewBlog(BlogDto blogDto, Long authorId, Long categoryId) {

        Categories category = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));


        Users author = userRepositories.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "Author Id", authorId));

        Blogs blog = modelMapper.map(blogDto, Blogs.class);
        blog.setCreatedAt(LocalDateTime.now());
        blog.setUpdatedAt(LocalDateTime.now());
        blog.setCategory(category);
        blog.setAuthor(author);
        blog = blogRepositories.save(blog);

        return modelMapper.map(blog, BlogDto.class);
    }

    @Override
    public List<BlogDto> getAllBlogs() {
        List<Blogs> blogs = blogRepositories.findAll();
        if (blogs.isEmpty()){
            throw new APIExceptionHandler("Blogs aren't created yet!!!");
        }
        return blogs.stream().map((blog)-> modelMapper.map(blog,BlogDto.class)).toList();
    }

    @Override
    public List<BlogDto> getBlogsByCategory(Long categoryId) {
        Categories category = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));

        List<Blogs> blogsByCategory = blogRepositories.findBlogsByCategory(category);

        return blogsByCategory
                .stream()
                .map(blogs -> modelMapper.map(blogs,BlogDto.class))
                .toList();
    }

    @Override
    public List<BlogDto> getBlogsByAuthor(Long authorId) {
        Users author = userRepositories.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "Author Id", authorId));
        List<Blogs> blogs = blogRepositories.findBlogsByAuthor(author);
        return blogs
                .stream()
                .map(blog -> modelMapper.map(blog, BlogDto.class))
                .toList();
    }


    @Override
    public BlogDto updateBlogDto(BlogDto blogDto,Long blogId,Long categoryId,Long authorId) {

        Blogs updatedBlog = blogRepositories.findById(blogId)
                .orElseThrow(()-> new ResourceNotFoundException("Blog","blog ID",blogId));
        Categories category = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        Users author = userRepositories.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "Author Id", authorId));
        updatedBlog.setCategory(category);
        updatedBlog.setAuthor(author);
        updatedBlog.setUpdatedAt(LocalDateTime.now());
        updatedBlog.setTitle(blogDto.getTitle());
        updatedBlog.setContent(blogDto.getContent());
        updatedBlog.setImageUrl(blogDto.getImageUrl());
        blogRepositories.save(updatedBlog);
        return modelMapper.map(updatedBlog, BlogDto.class);
    }

    @Override
    public BlogDto deleteBlog(Long blogID) {
        Blogs blogs = blogRepositories.findById(blogID).orElseThrow(()-> new ResourceNotFoundException("Blog","blog ID",blogID));
        BlogDto blogDto = modelMapper.map(blogs, BlogDto.class);
        blogRepositories.delete(blogs);
        return blogDto;
    }

    @Override
    public BlogDto getBlogById(Long blogId) {

        Blogs blog = blogRepositories.findById(blogId).orElseThrow(()-> new ResourceNotFoundException("blog","blogId",blogId));

        return modelMapper.map(blog, BlogDto.class);
    }


}
