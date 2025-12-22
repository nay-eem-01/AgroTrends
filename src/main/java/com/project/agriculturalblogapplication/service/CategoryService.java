package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.dtos.CategoryDto;
import com.project.agriculturalblogapplication.exceptionHandler.APIExceptionHandler;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.exceptionHandler.ResourceNotFoundException;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.repositories.CategoryRepositories;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepositories categoryRepositories;
    private final ModelMapper modelMapper;


    public CategoryService(CategoryRepositories categoryRepositories, ModelMapper modelMapper) {
        this.categoryRepositories = categoryRepositories;
        this.modelMapper = modelMapper;
    }

    public CategoryDto addNewCategory(CategoryDto categoryDto) {

        Category category = modelMapper.map(categoryDto, Category.class);


        Category savedCategory = categoryRepositories.findByCategoryName(categoryDto.getCategoryName());

        if (savedCategory != null) {
            throw new APIExceptionHandler("Category Already Exists");
        }
        savedCategory = categoryRepositories.save(category);

        return modelMapper.map(savedCategory, CategoryDto.class);
    }


    public List<CategoryDto> getAllCategories() {

        List<Category> categories = categoryRepositories.findAll();

        if (categories.isEmpty()) {
            throw new APIExceptionHandler("Categories haven't created yet !");
        }

        return categories
                .stream()
                .map(category -> modelMapper.map(category, CategoryDto.class))
                .toList();
    }


    public CategoryDto updateCategory(CategoryDto categoryDto, Long categoryId) {

        Category savedCategory = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        savedCategory.setCategoryName(categoryDto.getCategoryName());
        if (categoryDto.getCategoryName().equals(savedCategory.getCategoryName())) {
            throw new APIExceptionHandler("No changes made");
        }
        categoryRepositories.save(savedCategory);
        return modelMapper.map(savedCategory, CategoryDto.class);
    }


    public CategoryDto deleteCategory(Long categoryId) {
        Category category = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        CategoryDto categoryDto = modelMapper.map(category, CategoryDto.class);
        categoryRepositories.delete(category);
        return categoryDto;
    }

    public Category findByIdWithException(Long id){
        return categoryRepositories.findById(id).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_CATEGORY_NOT_FOUND));
    }

}
