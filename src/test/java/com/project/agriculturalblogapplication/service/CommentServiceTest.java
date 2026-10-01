package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Comment;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.response.CommentResponse;
import com.project.agriculturalblogapplication.repositories.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentServiceTest {

    private final BlogService blogService = mock(BlogService.class);
    private final UserService userService = mock(UserService.class);
    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final CommentService commentService =
            new CommentService(blogService, userService, commentRepository);

    private User author;
    private Blog blog;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(7L);
        blog = new Blog();
        blog.setId(3L);
    }

    private Comment comment(Long id, Comment parent) {
        Comment c = new Comment();
        c.setId(id);
        c.setBlog(blog);
        c.setUser(author);
        c.setParentComment(parent);
        c.setCommentContent("text");
        return c;
    }

    @Test
    void topLevelCommentMapsWithNullParentInsteadOfThrowing() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, null)));

        CommentResponse response = commentService.findById(10L);

        assertEquals(10L, response.getCommentId());
        assertNull(response.getParentCommentId());
        assertEquals(3L, response.getBlogId());
    }

    @Test
    void replyMapsItsParentId() {
        Comment parent = comment(10L, null);
        when(commentRepository.findById(11L)).thenReturn(Optional.of(comment(11L, parent)));

        assertEquals(10L, commentService.findById(11L).getParentCommentId());
    }


}
