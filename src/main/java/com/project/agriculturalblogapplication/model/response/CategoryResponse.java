package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Category;

public record CategoryResponse(Long id, String categoryName) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getCategoryName());
    }
}
