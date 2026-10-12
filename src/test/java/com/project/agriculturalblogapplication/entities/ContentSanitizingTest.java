package com.project.agriculturalblogapplication.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentSanitizingTest {

    private static final String DIRTY = "<p>ok</p><script>x</script>";

    @Test
    void everyUserHtmlFieldIsCleanedBeforeSaving() {
        Blog blog = new Blog();
        blog.setContent(DIRTY);
        Question question = new Question();
        question.setContent(DIRTY);
        Answer answer = new Answer();
        answer.setContent(DIRTY);
        Comment comment = new Comment();
        comment.setCommentContent(DIRTY);

        blog.sanitize();
        question.sanitize();
        answer.sanitize();
        comment.sanitize();

        assertEquals("<p>ok</p>", blog.getContent());
        assertEquals("<p>ok</p>", question.getContent());
        assertEquals("<p>ok</p>", answer.getContent());
        assertEquals("<p>ok</p>", comment.getCommentContent());
    }
}
