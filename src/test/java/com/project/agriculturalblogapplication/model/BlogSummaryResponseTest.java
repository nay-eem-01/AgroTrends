package com.project.agriculturalblogapplication.model;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.response.BlogSummaryResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogSummaryResponseTest {

    @Test
    void listsCarryAPlainTextExcerptNotTheBody() {
        assertTrue(Arrays.stream(BlogSummaryResponse.class.getRecordComponents()).noneMatch(c -> c.getName().equals("content")));

        assertEquals("Rice blast: spray early.", BlogSummaryResponse.from(blog("<h2>Rice blast:</h2><p>spray <b>early</b>.</p>")).excerpt());
    }

    @Test
    void longPostsAreCutAtAWordBoundary() {
        String excerpt = BlogSummaryResponse.from(blog("<p>" + "paddy field ".repeat(40) + "</p>")).excerpt();

        assertTrue(excerpt.length() <= 201, "length " + excerpt.length());
        assertTrue(excerpt.endsWith("field…") || excerpt.endsWith("paddy…"), excerpt);
    }

    private static Blog blog(String content) {
        User user = new User();
        user.setName("Rahim");
        Author author = new Author();
        author.setUser(user);
        Category category = new Category();
        Blog blog = new Blog();
        blog.setTitle("Rice blast");
        blog.setContent(content);
        blog.setAuthor(author);
        blog.setCategory(category);
        return blog;
    }
}
