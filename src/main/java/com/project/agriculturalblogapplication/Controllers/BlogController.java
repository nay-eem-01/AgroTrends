package com.project.agriculturalblogapplication.Controllers;


import com.project.agriculturalblogapplication.DTOS.BlogDto;
import com.project.agriculturalblogapplication.Service.BlogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("app/blog")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @PostMapping("/addNewBlog/user/{authorId}/category/{categoryId}")
    public ResponseEntity<BlogDto> addNewBlog(@RequestBody BlogDto blogDto, @PathVariable Long authorId, @PathVariable Long categoryId) {
        BlogDto savedBlog = blogService.addNewBlog(blogDto, authorId, categoryId);
        return new ResponseEntity<>(savedBlog, HttpStatus.CREATED);
    }

    @GetMapping("/getBlogs")
    public ResponseEntity<List<BlogDto>> getAllBlogs() {
        List<BlogDto> blogDtoList = blogService.getAllBlogs();
        return new ResponseEntity<>(blogDtoList, HttpStatus.OK);
    }
    @GetMapping("/getBlogs/category/{categoryId}")
    public ResponseEntity<List<BlogDto>> getBlogsByCategory(@PathVariable Long categoryId) {
        List<BlogDto> blogDtoList = blogService.getBlogsByCategory(categoryId);
        return new ResponseEntity<>(blogDtoList, HttpStatus.OK);
    }
    @GetMapping("/getBlogs/author/{authorId}")
    public ResponseEntity<List<BlogDto>> getBlogsByAuthor(@PathVariable Long authorId) {
        List<BlogDto> blogDtoList = blogService.getBlogsByAuthor(authorId);
        return new ResponseEntity<>(blogDtoList, HttpStatus.OK);
    }

    @PutMapping("/updateBlog/{blogId}/{AuthorId}/{categoryId}")
    public ResponseEntity<BlogDto> updateBlog(@RequestBody BlogDto blogDto,@PathVariable Long blogId,@PathVariable Long AuthorId, @PathVariable Long categoryId) {
        BlogDto updatedBlogDto = blogService.updateBlogDto(blogDto,blogId, AuthorId, categoryId);
        return new ResponseEntity<>(updatedBlogDto, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{blogId}")
    public ResponseEntity<BlogDto> deleteBlog(@PathVariable Long blogId){
        BlogDto blogDto = blogService.deleteBlog(blogId);
        return new ResponseEntity<>(blogDto,HttpStatus.OK);
    }
}
