package com.project.agriculturalblogapplication.Service;

import com.project.agriculturalblogapplication.DTOS.BlogDto;
import com.project.agriculturalblogapplication.Models.Blogs;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BlogService {
    BlogDto addNewBlog(BlogDto blogDto, Long authorId, Long categoryId);
    List<BlogDto> getAllBlogs ();
    List<BlogDto> getBlogsByCategory(Long categoryId);
    List<BlogDto> getBlogsByAuthor(Long authorId);
    BlogDto updateBlogDto(BlogDto blogDto,Long authorId,Long categoryId,Long authorID);
    BlogDto deleteBlog(Long blogID);

    BlogDto getBlogById(Long blogId);

}
