package com.project.agriculturalblogapplication;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.ExpertBadge;
import com.project.agriculturalblogapplication.entities.ReputationEvent;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.entities.TagFollow;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.enums.ReputationReason;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.AgriInfo;
import com.project.agriculturalblogapplication.model.response.ExpertSummaryResponse;
import com.project.agriculturalblogapplication.model.response.QuestionResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.security.service.CustomUserDetails;
import com.project.agriculturalblogapplication.service.AiService;
import com.project.agriculturalblogapplication.service.AuthorProfileService;
import com.project.agriculturalblogapplication.service.FollowService;
import com.project.agriculturalblogapplication.service.QuestionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AgriculturalBlogApplicationTests {

    @Autowired
    private EntityManager entities;

    @Autowired
    private FollowService follows;

    @Autowired
    private QuestionService questions;

    @Autowired
    private BlogRepositories blogs;

    @Autowired
    private AiService ai;

    @Autowired
    private AuthorProfileService profiles;

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    void categoryAndTagFollowsIncludeOnlyMatchingPublicContentAndReturnTheAuthorPhoto() {
        User reader = user("Test reader");
        User writer = user("Test writer");
        Author author = new Author();
        author.setUser(writer);
        author.setProfileImageUrl("https://images.example/writer.webp");
        entities.persist(author);
        Category rice = category("Test rice");
        Category soil = category("Test soil");
        Tag paddy = new Tag();
        paddy.setName("test paddy");
        entities.persist(paddy);
        TagFollow tagFollow = new TagFollow();
        tagFollow.setUser(reader);
        tagFollow.setTag(paddy);
        entities.persist(tagFollow);

        Question byCategory = question(writer, rice, false, Set.of());
        Question byTag = question(writer, soil, false, Set.of(paddy));
        Question byBoth = question(writer, rice, false, Set.of(paddy));
        question(writer, soil, false, Set.of());
        Question hidden = question(writer, rice, true, Set.of(paddy));
        Blog story = new Blog();
        story.setAuthor(author);
        story.setCategory(rice);
        story.setTitle("Test profile photo");
        story.setContent("Test text");
        entities.persist(story);
        entities.flush();
        entities.clear();

        CustomUserDetails principal = CustomUserDetails.build(reader);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        try {
            follows.followCategory(rice.getId(), "en");
            follows.followCategory(rice.getId(), "en");
            assertEquals(1, follows.followedCategories("en").size());

            var page = followedQuestions();
            assertEquals(Set.of(byCategory.getId(), byTag.getId(), byBoth.getId()), page.map(QuestionResponse::getQuestionId).toSet());
            assertTrue(page.stream().allMatch(question -> author.getProfileImageUrl().equals(question.getAuthorPhotoUrl())));
            assertEquals(story.getId(), blogs.findFollowingFeed(reader.getId(), PageRequest.of(0, 10)).getContent().getFirst().getId());
            assertNull(ai.questionAnswer(byCategory.getId(), "en"));
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApplicationException.class,
                    () -> ai.questionAnswer(hidden.getId(), "en")).getHttpStatus());

            follows.unfollowCategory(rice.getId(), "en");
            assertTrue(follows.followedCategories("en").isEmpty());
            assertEquals(Set.of(byTag.getId(), byBoth.getId()), followedQuestions().map(QuestionResponse::getQuestionId).toSet());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @Transactional
    void recommendsUnfollowedExpertsByReputationInFollowedCategoriesAndPublicTags() {
        User reader = user("Recommendation reader");
        User riceExpert = user("Rice expert");
        User soilExpert = user("Soil expert");
        User followedExpert = user("Already followed");
        User unrelatedExpert = user("Hidden topic expert");
        Category rice = category("Recommendation rice");
        Category soil = category("Recommendation soil");
        Category hidden = category("Recommendation hidden");
        Tag tag = new Tag();
        tag.setName("recommendation test");
        entities.persist(tag);
        TagFollow tagFollow = new TagFollow();
        tagFollow.setUser(reader);
        tagFollow.setTag(tag);
        entities.persist(tagFollow);
        question(reader, soil, false, Set.of(tag));
        question(reader, rice, false, Set.of(tag));
        question(reader, hidden, true, Set.of(tag));

        expert(riceExpert, rice, 12);
        expert(soilExpert, soil, 25);
        reputation(soilExpert, soil, -5);
        reputation(riceExpert, hidden, 100);
        expert(followedExpert, rice, 200);
        expert(reader, rice, 200);
        expert(unrelatedExpert, hidden, 200);

        CustomUserDetails principal = CustomUserDetails.build(reader);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        try {
            follows.followCategory(rice.getId(), "en");
            follows.followAuthor(followedExpert.getId(), "en");
            entities.flush();

            var recommendations = profiles.recommendedExperts("en");
            assertEquals(List.of(soilExpert.getId(), riceExpert.getId()),
                    recommendations.stream().map(ExpertSummaryResponse::userId).toList());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void expert(User user, Category category, int points) {
        Author author = new Author();
        author.setUser(user);
        entities.persist(author);
        ExpertBadge badge = new ExpertBadge();
        badge.setUser(user);
        badge.setTopic(category);
        badge.setFieldTitle("Agronomy");
        entities.persist(badge);
        reputation(user, category, points);
    }

    private void reputation(User user, Category category, int points) {
        ReputationEvent event = new ReputationEvent();
        event.setUser(user);
        event.setTopic(category);
        event.setReason(ReputationReason.ANSWER_ACCEPTED);
        event.setPoints(points);
        entities.persist(event);
    }

    private Page<QuestionResponse> followedQuestions() {
        return questions.getAll(new PaginationArgs(0, 10, "creationDate", AscOrDescType.desc),
                null, null, false, true, new AgriInfo(null, null, null, null), "en");
    }

    private User user(String name) {
        User user = new User();
        user.setName(name);
        user.setRoles(Set.of());
        entities.persist(user);
        return user;
    }

    private Category category(String name) {
        Category category = new Category();
        category.setCategoryName(name);
        entities.persist(category);
        return category;
    }

    private Question question(User writer, Category category, boolean hidden, Set<Tag> tags) {
        Question question = new Question();
        question.setUser(writer);
        question.setCategory(category);
        question.setTitle("Test question");
        question.setContent("Test text");
        question.setHidden(hidden);
        question.setTags(tags);
        entities.persist(question);
        return question;
    }
}
