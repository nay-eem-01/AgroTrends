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
}
