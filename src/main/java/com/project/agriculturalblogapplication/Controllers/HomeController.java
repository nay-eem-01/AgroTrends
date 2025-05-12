package com.project.agriculturalblogapplication.Controllers;



import com.project.agriculturalblogapplication.DTOS.BlogDto;
import com.project.agriculturalblogapplication.DTOS.CategoryDto;
import com.project.agriculturalblogapplication.Service.BlogService;
import com.project.agriculturalblogapplication.Service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("app/")
public class HomeController {

    private final BlogService blogService;
    private final CategoryService categoryService;

    @Autowired
    public HomeController(BlogService blogService, CategoryService categoryService) {
        this.blogService = blogService;
        this.categoryService = categoryService;
    }

    @GetMapping("/home")
    public String home(Model model) {
        List<BlogDto> blogs = blogService.getAllBlogs(); // You can add pagination or limit for latest
        List<CategoryDto> categories = categoryService.getAllCategories();

        model.addAttribute("blogs", blogs);
        model.addAttribute("categories", categories);
        return "home"; // maps to src/main/resources/templates/home.html
    }
}
