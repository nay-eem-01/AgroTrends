package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.events.QuestionCreated;
import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.CropSeason;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.enums.SoilType;
import com.project.agriculturalblogapplication.model.AgriInfo;
import com.project.agriculturalblogapplication.model.request.CreateQuestionRequest;
import com.project.agriculturalblogapplication.model.request.UpdateQuestionRequest;
import com.project.agriculturalblogapplication.model.response.QuestionResponse;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionServiceTest {

    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final UserService userService = mock(UserService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final TagService tagService = mock(TagService.class);
    private final ImageService imageService = mock(ImageService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final QuestionService questionService =
            new QuestionService(questionRepository, userService, authorization, categoryService, tagService, imageService,
                    mock(ReputationService.class), events);

    @Test
    void trendingQuestionsAreTheLastTwoWeeksHighestScoreThenMostAnswered() {
        ArgumentCaptor<java.time.LocalDateTime> since = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        when(questionRepository.findAllByHiddenFalseAndCreationDateAfter(since.capture(), page.capture()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        questionService.trending(0, 10);

        assertTrue(since.getValue().isBefore(java.time.LocalDateTime.now().minusDays(13)));
        assertEquals(Sort.by(Sort.Direction.DESC, "score", "answerCount", "creationDate", "id"), page.getValue().getSort());
    }

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
    void aQuestionHasATopicAndTagsAndAnUpdateWithoutTagsKeepsThem() {
        User user = new User();
        user.setId(7L);
        Category rice = new Category();
        rice.setId(2L);
        rice.setCategoryName("Rice");
        Tag blast = new Tag();
        blast.setName("rice blast");
        when(authorization.currentUserId("en")).thenReturn(7L);
        when(userService.findByIdWithException(7L, "en")).thenReturn(user);
        when(categoryService.findByIdWithException(2L)).thenReturn(rice);
        when(tagService.resolve(List.of("Rice Blast"))).thenReturn(Set.of(blast));
        when(questionRepository.save(any(Question.class))).thenAnswer(call -> call.getArgument(0));
        CreateQuestionRequest create = new CreateQuestionRequest();
        create.setTitle("Brown spots");
        create.setContent("Why?");
        create.setCategoryId(2L);
        create.setTags(List.of("Rice Blast"));

        when(questionRepository.save(any(Question.class))).thenAnswer(call -> {
            Question saved = call.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        QuestionResponse created = questionService.create(create, "en");
        verify(events).publishEvent(new QuestionCreated(11L));
        assertEquals("Rice", created.getCategory().categoryName());
        assertEquals(List.of("rice blast"), created.getTags());

        Question stored = new Question();
        stored.setId(4L);
        stored.setUser(user);
        stored.setTags(Set.of(blast));
        when(questionRepository.findById(4L)).thenReturn(Optional.of(stored));
        UpdateQuestionRequest update = new UpdateQuestionRequest();
        update.setQuestionId(4L);
        update.setTitle("Brown spots on leaves");
        update.setContent("Still why?");
        update.setCategoryId(2L);

        assertEquals(List.of("rice blast"), questionService.update(update, "en").getTags());
    }

    @Test
    void aHiddenQuestionIsNotFoundForReadsAndEditsButCanStillBeDeleted() {
        User owner = new User();
        owner.setId(7L);
        Question hidden = new Question();
        hidden.setId(4L);
        hidden.setUser(owner);
        hidden.setHidden(true);
        when(questionRepository.findById(4L)).thenReturn(Optional.of(hidden));

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApplicationException.class, () -> questionService.findById(4L)).getHttpStatus());

        questionService.delete(4L, "en");
        verify(questionRepository).delete(hidden);
    }

    @Test
    void searchNeedsSomethingToSearchFor() {
        ApplicationException refused = assertThrows(ApplicationException.class, () -> questionService.search("  ", 0, 10, "en"));
        assertEquals(HttpStatus.BAD_REQUEST, refused.getHttpStatus());
    }

    @Test
    void aQuestionAskedBeforeTopicsExistedHasNoTopic() {
        User user = new User();
        user.setId(7L);
        Question legacy = new Question();
        legacy.setId(4L);
        legacy.setUser(user);
        when(questionRepository.findById(4L)).thenReturn(Optional.of(legacy));

        assertNull(questionService.findById(4L).getCategory());
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
        create.setCategoryId(2L);
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
        update.setCategoryId(2L);

        assertEquals("bogura", questionService.update(update, "en").getAgri().region());
    }
}
