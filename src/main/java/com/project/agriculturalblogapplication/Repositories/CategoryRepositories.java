package com.project.agriculturalblogapplication.Repositories;

import com.project.agriculturalblogapplication.Models.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepositories extends JpaRepository<Categories, Long> {
    Categories findByCategoryName(String categoryName);
}
