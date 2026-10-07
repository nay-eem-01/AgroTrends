package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.response.QuestionResponse;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionServiceTest {

    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final QuestionService questionService =
            new QuestionService(questionRepository, mock(UserService.class), mock(AuthorizationService.class));

    @Test
    void questionShowsItsAuthorsNameAndIsoTimestamps() {
        User user = new User();
        user.setId(7L);
        user.setName("Karim");
        user.setEmail("karim@example.com");
        Question question = new Question();
        question.setId(4L);
        question.setTitle("Yellow leaves");
        question.setContent("Why?");
        question.setUser(user);
        question.setCreationDate(LocalDateTime.of(2026, 10, 7, 21, 15));
        when(questionRepository.findById(4L)).thenReturn(Optional.of(question));

        QuestionResponse response = questionService.findById(4L);

        assertEquals("Karim", response.getAuthorName());
        // Regression: dates were "dd-MM-yyyy hh:mm:ss" (12-hour, no AM/PM): 9:15 pm read as 09:15.
        assertEquals(LocalDateTime.of(2026, 10, 7, 21, 15).atZone(ZoneId.systemDefault()).toInstant(), response.getCreatedAt());
        assertNull(response.getUpdatedAt());
    }
}
