package com.project.agriculturalblogapplication.config;

import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.repositories.CategoryRepositories;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TopicSeederTest {
    @Test
    @SuppressWarnings("unchecked")
    void seedsFortyDistinctTopicsOnlyWhenEmpty() {
        CategoryRepositories categories = mock(CategoryRepositories.class);
        when(categories.count()).thenReturn(0L, 40L);
        TopicSeeder seeder = new TopicSeeder(categories);
        seeder.seed();
        seeder.seed();
        ArgumentCaptor<List<Category>> saved = ArgumentCaptor.forClass(List.class);
        verify(categories).saveAll(saved.capture());
        assertEquals(40, saved.getValue().size());
        assertEquals(40, saved.getValue().stream().map(Category::getCategoryName).distinct().count());
        assertTrue(saved.getValue().stream().anyMatch(c -> c.getCategoryName().equals("General agriculture")));
    }

    @Test
    void preservesAnExistingCuratedTopicList() {
        CategoryRepositories categories = mock(CategoryRepositories.class);
        when(categories.count()).thenReturn(1L);
        new TopicSeeder(categories).seed();
        verify(categories, never()).saveAll(any());
    }
}
