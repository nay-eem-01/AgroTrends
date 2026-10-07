package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.CropSeason;
import com.project.agriculturalblogapplication.enums.SoilType;
import com.project.agriculturalblogapplication.model.AgriInfo;
import com.project.agriculturalblogapplication.model.request.CreateQuestionRequest;
import com.project.agriculturalblogapplication.model.request.UpdateQuestionRequest;
import com.project.agriculturalblogapplication.model.response.QuestionResponse;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionServiceTest {

    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final UserService userService = mock(UserService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final QuestionService questionService = new QuestionService(questionRepository, userService, authorization);

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

    @Test
    void aQuestionKeepsItsAgriInfoAndAnUpdateWithoutItKeepsIt() {
        User user = new User();
        user.setId(7L);
        user.setName("Karim");
        when(authorization.currentUserId("en")).thenReturn(7L);
        when(userService.findByIdWithException(7L, "en")).thenReturn(user);
        when(questionRepository.save(any(Question.class))).thenAnswer(call -> call.getArgument(0));
        CreateQuestionRequest create = new CreateQuestionRequest();
        create.setTitle("Yellow leaves");
        create.setContent("Why?");
        create.setAgri(new AgriInfo(" Aman Rice", CropSeason.KHARIF_2, "Bogura", SoilType.CLAY));

        QuestionResponse created = questionService.create(create, "en");
        assertEquals(new AgriInfo("aman rice", CropSeason.KHARIF_2, "bogura", SoilType.CLAY), created.getAgri());

        Question stored = new Question();
        stored.setId(4L);
        stored.setUser(user);
        stored.setAgri(create.getAgri().toMetadata());
        when(questionRepository.findById(4L)).thenReturn(Optional.of(stored));
        UpdateQuestionRequest update = new UpdateQuestionRequest();
        update.setQuestionId(4L);
        update.setTitle("Yellow lower leaves");
        update.setContent("Still why?");

        assertEquals("bogura", questionService.update(update, "en").getAgri().region());
    }
}
