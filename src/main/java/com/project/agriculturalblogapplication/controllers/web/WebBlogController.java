//package com.project.agriculturalblogapplication.controllers.web;
//
//
//import com.project.agriculturalblogapplication.dtos.BlogDto;
//import com.project.agriculturalblogapplication.service.BlogService;
//import com.project.agriculturalblogapplication.service.CategoryService;
//import com.project.agriculturalblogapplication.service.CommentService;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@Controller
//@RequestMapping("app/blog")
//public class WebBlogController {
//
//    private final BlogService blogService;
//    private final CategoryService categoryService;
//    private final CommentService commentService;
//
//    public WebBlogController(BlogService blogService, CategoryService categoryService, CommentService commentService) {
//        this.blogService = blogService;
//        this.categoryService = categoryService;
//        this.commentService = commentService;
//    }
//
//
//
//    @GetMapping("/blog_page")
//    public String showForm(Model model){
//
//        model.addAttribute("blogs",blogService.getAll());
//        model.addAttribute("categories",categoryService.getAllCategories());
//
//        return "/blog";
//    }
//
//
//    @GetMapping("/details/{blogId}")
//    public String showBlogDetailsPage(@PathVariable Long blogId, Model model){
//
//        BlogDto blogDto = blogService.findByIdWithException(blogId);
//
//        model.addAttribute("blogs",blogService.getAll());
//        model.addAttribute("categories",categoryService.getAllCategories());
//        model.addAttribute("comments",commentService.viewAllCommentsByBlogId(blogId));
//        model.addAttribute("blog",blogDto);
//
//        return "/blog_details";
//    }
//
//    @PostMapping("/addNewBlog/user/{authorId}/category/{categoryId}")
//    public ResponseEntity<BlogDto> addNewBlog(@RequestBody BlogDto blogDto, @PathVariable Long authorId, @PathVariable Long categoryId) {
//        BlogDto savedBlog = blogService.create(blogDto, authorId, categoryId);
//        return new ResponseEntity<>(savedBlog, HttpStatus.CREATED);
//    }
//
//    @GetMapping("/getAllBlogs")
//    public ResponseEntity<List<BlogDto>> getAllBlogs() {
//        List<BlogDto> blogDtoList = blogService.getAll();
//        return new ResponseEntity<>(blogDtoList, HttpStatus.OK);
//    }
//    @GetMapping("/getBlogs/category/{categoryId}")
//    public ResponseEntity<List<BlogDto>> getBlogsByCategory(@PathVariable Long categoryId) {
//        List<BlogDto> blogDtoList = blogService.getAllByCategory(categoryId);
//        return new ResponseEntity<>(blogDtoList, HttpStatus.OK);
//    }
//
//
//
//    @GetMapping("/getBlogs/author/{authorId}")
//    public ResponseEntity<List<BlogDto>> getBlogsByAuthor(@PathVariable Long authorId) {
//        List<BlogDto> blogDtoList = blogService.getAllByAuthor(authorId);
//        return new ResponseEntity<>(blogDtoList, HttpStatus.OK);
//    }
//
//    @PutMapping("/updateBlog/{blogId}/{AuthorId}/{categoryId}")
//    public ResponseEntity<BlogDto> updateBlog(@RequestBody BlogDto blogDto,@PathVariable Long blogId,@PathVariable Long AuthorId, @PathVariable Long categoryId) {
//        BlogDto updatedBlogDto = blogService.update(blogDto,blogId, AuthorId, categoryId);
//        return new ResponseEntity<>(updatedBlogDto, HttpStatus.OK);
//    }
//
//    @DeleteMapping("/delete/{blogId}")
//    public ResponseEntity<BlogDto> deleteBlog(@PathVariable Long blogId){
//        BlogDto blogDto = blogService.delete(blogId);
//        return new ResponseEntity<>(blogDto,HttpStatus.OK);
//    }
//}
