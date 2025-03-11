package com.project.agriculturalblogapplication.Service;

import com.project.agriculturalblogapplication.DTOS.CategoryDto;
import com.project.agriculturalblogapplication.Models.Categories;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CategoryService {

    CategoryDto addNewCategory(CategoryDto categoryDto);
    List<CategoryDto> getAllCategories();
    CategoryDto updateCategory(CategoryDto categoryDto,Long categoryId);
    CategoryDto deleteCategory(Long categoryId);


}
