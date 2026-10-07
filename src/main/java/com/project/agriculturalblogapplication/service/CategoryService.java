package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.model.response.CategoryResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.CategoryRepositories;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;



@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepositories categoryRepositories;

    public CategoryResponse create(String categoryName) {
        if (categoryRepositories.existsByCategoryName(categoryName)){
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_CATEGORY_ALREADY_EXISTS);
        }

        Category category = new Category();
        category.setCategoryName(categoryName);

        return CategoryResponse.from(categoryRepositories.save(category));
    }

    public Page<CategoryResponse> getAll(PaginationArgs paginationArgs, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs, Set.of("creationDate", "categoryName"), lang);
        return categoryRepositories.findAll(pageable).map(CategoryResponse::from);
    }

    public CategoryResponse update(String categoryName, Long categoryId) {
        Category category = findByIdWithException(categoryId);
        category.setCategoryName(categoryName);

        return CategoryResponse.from(categoryRepositories.save(category));
    }

    public void delete(Long categoryId) {
        Category category = findByIdWithException(categoryId);
        categoryRepositories.delete(category);
    }

    public Category findByIdWithException(Long id){
        return categoryRepositories.findById(id).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_CATEGORY_NOT_FOUND));
    }
}
