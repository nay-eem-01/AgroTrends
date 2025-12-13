package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.DTOS.CategoryDto;
import com.project.agriculturalblogapplication.ExceptionHandler.APIExceptionHandler;
import com.project.agriculturalblogapplication.ExceptionHandler.ResourceNotFoundException;
import com.project.agriculturalblogapplication.entities.Categories;
import com.project.agriculturalblogapplication.Repositories.CategoryRepositories;
import org.modelmapper.ModelMapper;
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

        Categories category = modelMapper.map(categoryDto, Categories.class);


        Categories savedCategory = categoryRepositories.findByCategoryName(categoryDto.getCategoryName());

        if (savedCategory != null) {
            throw new APIExceptionHandler("Category Already Exists");
        }
        savedCategory = categoryRepositories.save(category);

        return modelMapper.map(savedCategory, CategoryDto.class);
    }


    public List<CategoryDto> getAllCategories() {

        List<Categories> categories = categoryRepositories.findAll();

        if (categories.isEmpty()) {
            throw new APIExceptionHandler("Categories haven't created yet !");
        }

        return categories
                .stream()
                .map(category -> modelMapper.map(category, CategoryDto.class))
                .toList();
    }


    public CategoryDto updateCategory(CategoryDto categoryDto, Long categoryId) {

        Categories savedCategory = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        savedCategory.setCategoryName(categoryDto.getCategoryName());
        if (categoryDto.getCategoryName().equals(savedCategory.getCategoryName())) {
            throw new APIExceptionHandler("No changes made");
        }
        categoryRepositories.save(savedCategory);
        return modelMapper.map(savedCategory, CategoryDto.class);
    }


    public CategoryDto deleteCategory(Long categoryId) {
        Categories category = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        CategoryDto categoryDto = modelMapper.map(category, CategoryDto.class);
        categoryRepositories.delete(category);
        return categoryDto;
    }

}
