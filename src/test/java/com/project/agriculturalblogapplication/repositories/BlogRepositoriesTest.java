package com.project.agriculturalblogapplication.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogRepositoriesTest {

    @Test
    void blogQueriesRunInAReadOnlyTransaction() {
        // Regression: paged derived queries (findAllByStatus, ...) failed with "Large Objects may not be used in
        // auto-commit mode" because Blog.content is a @Lob read outside a transaction.
        Transactional transactional = BlogRepositories.class.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }
}
