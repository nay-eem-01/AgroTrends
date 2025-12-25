//package com.project.agriculturalblogapplication.controllers.web;
//
//import com.project.agriculturalblogapplication.dtos.CategoryDto;
//import com.project.agriculturalblogapplication.service.CategoryService;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/app/category")
//public class WebCategoryController {
//    private final CategoryService categoryService;
//
//    public WebCategoryController(CategoryService categoryService) {
//        this.categoryService = categoryService;
//    }
//
//
//    @PostMapping("/newCategory")
//    public ResponseEntity<CategoryDto> addNewCategory(@RequestBody String categoryName) {
//        CategoryDto savedCategoryDto = categoryService.create(categoryName);
//        return new ResponseEntity<>(categoryName, HttpStatus.CREATED);
//    }
//
//    @GetMapping("/getAllCategory")
//    public ResponseEntity<List<CategoryDto>> getAllCategories(){
//        List<CategoryDto> categoryDtos = categoryService.getAllCategories();
//        return new ResponseEntity<>(categoryDtos,HttpStatus.OK);
//    }
//    @PutMapping("/update/{categoryId}")
//    public ResponseEntity<CategoryDto> updateCategory(@RequestBody CategoryDto categoryDto,@PathVariable Long categoryId){
//        CategoryDto updatedcategoryDto = categoryService.updateCategory(categoryDto,categoryId);
//        return new ResponseEntity<>(updatedcategoryDto,HttpStatus.CREATED);
//    }
//    @DeleteMapping("/delete/{categoryId}")
//    public ResponseEntity<CategoryDto> deleteCategory(@PathVariable Long categoryId){
//        CategoryDto categoryDto = categoryService.deleteCategory(categoryId);
//        return new ResponseEntity<>(categoryDto,HttpStatus.ACCEPTED);
//    }
//}
