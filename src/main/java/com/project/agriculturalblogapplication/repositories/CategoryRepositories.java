package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepositories extends JpaRepository<Category, Long> {

    Category findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);
}
