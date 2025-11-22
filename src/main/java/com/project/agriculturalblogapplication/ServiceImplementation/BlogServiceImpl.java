package com.project.agriculturalblogapplication.ServiceImplementation;

import com.project.agriculturalblogapplication.DTOS.BlogDto;
import com.project.agriculturalblogapplication.ExceptionHandler.APIExceptionHandler;
import com.project.agriculturalblogapplication.ExceptionHandler.ResourceNotFoundException;
import com.project.agriculturalblogapplication.Repositories.AuthorRepository;
import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Categories;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.Repositories.BlogRepositories;
import com.project.agriculturalblogapplication.Repositories.CategoryRepositories;
import com.project.agriculturalblogapplication.Repositories.UserRepository;
import com.project.agriculturalblogapplication.Service.BlogService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {


    private final BlogRepositories blogRepositories;
    private final CategoryRepositories categoryRepositories;
    private final AuthorRepository authorRepository;
    private final ModelMapper modelMapper;



    @Override
    public BlogDto addNewBlog(BlogDto blogDto, Long authorId, Long categoryId) {

        Categories category = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));


        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "Author Id", authorId));

        Blog blog = modelMapper.map(blogDto, Blog.class);

        blog.setCategory(category);
        blog.setAuthor(author);
        blog = blogRepositories.save(blog);

        return modelMapper.map(blog, BlogDto.class);
    }

    @Override
    public List<BlogDto> getAllBlogs() {
        List<Blog> blogs = blogRepositories.findAll();
        if (blogs.isEmpty()){
            throw new APIExceptionHandler("Blogs aren't created yet!!!");
        }
        return blogs.stream().map((blog)-> modelMapper.map(blog,BlogDto.class)).toList();
    }

    @Override
    public List<BlogDto> getBlogsByCategory(Long categoryId) {
        Categories category = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));

        List<Blog> blogByCategory = blogRepositories.findBlogsByCategory(category);

        return blogByCategory
                .stream()
                .map(blogs -> modelMapper.map(blogs,BlogDto.class))
                .toList();
    }

    @Override
    public List<BlogDto> getBlogsByAuthor(Long authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "Author Id", authorId));
        List<Blog> blogs = blogRepositories.findBlogsByAuthor(author);
        return blogs
                .stream()
                .map(blog -> modelMapper.map(blog, BlogDto.class))
                .toList();
    }


    @Override
    public BlogDto updateBlogDto(BlogDto blogDto,Long blogId,Long categoryId,Long authorId) {

        Blog updatedBlog = blogRepositories.findById(blogId)
                .orElseThrow(()-> new ResourceNotFoundException("Blog","blog ID",blogId));

        Categories category = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));

        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "Author Id", authorId));

        updatedBlog.setCategory(category);
        updatedBlog.setAuthor(author);
        updatedBlog.setTitle(blogDto.getTitle());
        updatedBlog.setContent(blogDto.getContent());
        updatedBlog.setImageUrl(blogDto.getImageUrl());
        blogRepositories.save(updatedBlog);

        return modelMapper.map(updatedBlog, BlogDto.class);
    }

    @Override
    public BlogDto deleteBlog(Long blogID) {
        Blog blog = blogRepositories.findById(blogID).orElseThrow(()-> new ResourceNotFoundException("Blog","blog ID",blogID));
        BlogDto blogDto = modelMapper.map(blog, BlogDto.class);
        blogRepositories.delete(blog);
        return blogDto;
    }

    @Override
    public BlogDto getBlogById(Long blogId) {

        Blog blog = blogRepositories.findById(blogId).orElseThrow(()-> new ResourceNotFoundException("blog","blogId",blogId));

        return modelMapper.map(blog, BlogDto.class);
    }


}
