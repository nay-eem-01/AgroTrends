package com.project.agriculturalblogapplication.Controllers;

import com.project.agriculturalblogapplication.DTOS.CategoryDto;
import com.project.agriculturalblogapplication.Service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/category")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }


    @PostMapping("/newCategory")
    public ResponseEntity<CategoryDto> addNewCategory(@RequestBody CategoryDto categoryDto) {
        CategoryDto savedCategoryDto = categoryService.addNewCategory(categoryDto);
        return new ResponseEntity<>(savedCategoryDto, HttpStatus.CREATED);
    }

    @GetMapping("/getCategory")
    public ResponseEntity<List<CategoryDto>> getAllCategories(){
        List<CategoryDto> categoryDtos = categoryService.getAllCategories();
        return new ResponseEntity<>(categoryDtos,HttpStatus.OK);
    }
    @PutMapping("/update/{categoryId}")
    public ResponseEntity<CategoryDto> updateCategory(@RequestBody CategoryDto categoryDto,@PathVariable Long categoryId){
        CategoryDto updatedcategoryDto = categoryService.updateCategory(categoryDto,categoryId);
        return new ResponseEntity<>(updatedcategoryDto,HttpStatus.CREATED);
    }
    @DeleteMapping("/delete/{categoryId}")
    public ResponseEntity<CategoryDto> deleteCategory(@PathVariable Long categoryId){
        CategoryDto categoryDto = categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(categoryDto,HttpStatus.ACCEPTED);
    }
}
