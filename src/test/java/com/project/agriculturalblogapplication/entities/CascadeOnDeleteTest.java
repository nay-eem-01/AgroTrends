package com.project.agriculturalblogapplication.entities;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CascadeOnDeleteTest {

    @Test
    void clapsGoWithTheirBlogAndUser() throws Exception {
        // Regression: claps.blog_id had no ON DELETE, so deleting a clapped blog failed on the foreign key.
        assertEquals(OnDeleteAction.CASCADE, Clap.class.getDeclaredField("blog").getAnnotation(OnDelete.class).action());
        assertEquals(OnDeleteAction.CASCADE, Clap.class.getDeclaredField("user").getAnnotation(OnDelete.class).action());
    }

    @Test
    void bookmarksGoWithTheirBlogAndUser() throws Exception {
        assertEquals(OnDeleteAction.CASCADE, Bookmark.class.getDeclaredField("blog").getAnnotation(OnDelete.class).action());
        assertEquals(OnDeleteAction.CASCADE, Bookmark.class.getDeclaredField("user").getAnnotation(OnDelete.class).action());
    }

    @Test
    void commentsAndTheirRepliesGoWithTheirBlog() throws Exception {
        // Regression: deleting a blog with comments failed on comment.blog_id (answered as a vague 400).
        assertEquals(OnDeleteAction.CASCADE, Comment.class.getDeclaredField("blog").getAnnotation(OnDelete.class).action());
        assertEquals(OnDeleteAction.CASCADE, Comment.class.getDeclaredField("parentComment").getAnnotation(OnDelete.class).action());
    }
}
