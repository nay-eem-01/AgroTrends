package com.project.agriculturalblogapplication.ServiceImplementation;

import com.project.agriculturalblogapplication.DTOS.CategoryDto;
import com.project.agriculturalblogapplication.ExceptionHandler.APIExceptionHandler;
import com.project.agriculturalblogapplication.ExceptionHandler.ResourceNotFoundException;
import com.project.agriculturalblogapplication.Models.Categories;
import com.project.agriculturalblogapplication.Repositories.CategoryRepositories;
import com.project.agriculturalblogapplication.Service.CategoryService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepositories categoryRepositories;
    private final ModelMapper modelMapper;


    public CategoryServiceImpl(CategoryRepositories categoryRepositories, ModelMapper modelMapper) {
        this.categoryRepositories = categoryRepositories;
        this.modelMapper = modelMapper;
    }

    @Override
    public CategoryDto addNewCategory(CategoryDto categoryDto) {

        Categories category = modelMapper.map(categoryDto, Categories.class);


        Categories savedCategory = categoryRepositories.findByCategoryName(categoryDto.getCategoryName());

        if (savedCategory != null) {
            throw new APIExceptionHandler("Category Already Exists");
        }
        savedCategory = categoryRepositories.save(category);

        return modelMapper.map(savedCategory, CategoryDto.class);
    }

    @Override
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

    @Override
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

    @Override
    public CategoryDto deleteCategory(Long categoryId) {
        Categories category = categoryRepositories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        CategoryDto categoryDto = modelMapper.map(category, CategoryDto.class);
        categoryRepositories.delete(category);
        return categoryDto;
    }

}
