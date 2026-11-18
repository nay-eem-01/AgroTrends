package com.project.agriculturalblogapplication.model;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.response.AuthorSummaryResponse;
import com.project.agriculturalblogapplication.model.response.BlogResponse;
import com.project.agriculturalblogapplication.model.response.CategoryResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogResponseTest {

    @Test
    void aBlogShowsItsAuthorByNameOnly() {
        User user = new User();
        user.setId(10L);
        user.setName("Rahim");
        user.setEmail("rahim@example.com");
        user.setMobileNumber("01700000000");
        Author author = new Author();
        author.setId(20L);
        author.setUser(user);
        Category category = new Category();
        category.setId(30L);
        category.setCategoryName("Crop diseases");
        Blog blog = new Blog();
        blog.setId(1L);
        blog.setTitle("Rice blast");
        blog.setContent("Spray early.");
        blog.setAuthor(author);
        blog.setCategory(category);
        blog.setCreationDate(LocalDateTime.of(2026, 10, 7, 9, 30));

        BlogResponse response = BlogResponse.from(blog);

        assertEquals(new AuthorSummaryResponse(20L, "Rahim"), response.author());
        assertEquals(new CategoryResponse(30L, "Crop diseases"), response.category());
        assertEquals(LocalDateTime.of(2026, 10, 7, 9, 30).atZone(ZoneId.systemDefault()).toInstant(), response.createdAt());
        assertNull(response.updatedAt());
        // Regression: the entity used to serialize author.user with e-mail, mobile number and roles.
        assertTrue(Arrays.stream(AuthorSummaryResponse.class.getRecordComponents())
                .allMatch(c -> Set.of("authorId", "name").contains(c.getName())));
        assertTrue(Arrays.stream(BlogResponse.class.getRecordComponents())
                .noneMatch(c -> c.getType() == User.class || c.getType() == Author.class));
    }
}
