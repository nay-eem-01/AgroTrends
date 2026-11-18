package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AppProperties;
import com.project.agriculturalblogapplication.entities.DigestSent;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.mail.Mail;
import com.project.agriculturalblogapplication.mail.MailService;
import com.project.agriculturalblogapplication.repositories.DigestSentRepository;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.repositories.TagFollowRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.UnsubscribeTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The weekly digest (frontend request B10). */
class DigestServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final TagFollowRepository tagFollows = mock(TagFollowRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionAiAnswerRepository aiAnswers = mock(QuestionAiAnswerRepository.class);
    private final DigestSentRepository sent = mock(DigestSentRepository.class);
    private final ExpertService experts = mock(ExpertService.class);
    private final MailService mail = mock(MailService.class);
    private DigestService digest;
    private User user;

    @BeforeEach
    void setUp() {
        AppProperties app = new AppProperties();
        app.setBackendUrl("https://api.agrotrends.app");
        digest = new DigestService(users, tagFollows, questions, aiAnswers, sent, experts, mock(AuthorizationService.class),
                mail, new UnsubscribeTokens("a-test-secret-that-is-at-least-32-characters"), app,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
        ReflectionTestUtils.setField(digest, "frontendUrl", "https://agrotrends.app");
        user = new User();
        user.setId(7L);
        user.setEmail("answerer@example.com");
        user.setPreferredLang("en");
        when(users.digestRecipientIds()).thenReturn(List.of(7L));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(experts.verifiedTopics(Set.of(7L))).thenReturn(Map.of());
        when(tagFollows.followedTagIds(7L)).thenReturn(List.of());
    }

    private static Question question(long id, String title) {
        Question question = new Question();
        question.setId(id);
        question.setTitle(title);
        return question;
    }

    @Test
    void nothingToShowMeansNoMail() {
        digest.sendWeekly();

        verify(mail, never()).send(any());
        verify(sent, never()).saveAndFlush(any());
    }

    @Test
    void unansweredQuestionsInFollowedTagsAreListedWithLinks() {
        when(tagFollows.followedTagIds(7L)).thenReturn(List.of(3L));
        when(questions.unansweredWithTagsSince(eq(List.of(3L)), any(LocalDateTime.class), eq(7L), any()))
                .thenReturn(List.of(question(5L, "Yellow rice leaves")));

        Mail mailed = digest.digestFor(user).orElseThrow();

        assertTrue(mailed.text().contains("• Yellow rice leaves\n  https://agrotrends.app/questions/5"));
        assertEquals("https://agrotrends.app/questions", mailed.actionUrl());
        assertTrue(mailed.unsubscribeUrl().endsWith("&kind=digest"));
    }

    @Test
    void anExpertHearsAboutTheirReviewQueue() {
        user.setPreferredLang("bn");
        when(experts.verifiedTopics(Set.of(7L))).thenReturn(Map.of(7L, Set.of(2L)));
        when(aiAnswers.countByStatusAndQuestionCategoryIdInAndQuestionHiddenFalse(AiAnswerStatus.UNCHECKED, Set.of(2L))).thenReturn(4L);
        QuestionAiAnswer oldest = new QuestionAiAnswer();
        oldest.setCreationDate(LocalDateTime.now().minusDays(3).minusHours(1));
        when(aiAnswers.findFirstByStatusAndQuestionCategoryIdInAndQuestionHiddenFalseOrderByCreationDateAsc(
                eq(AiAnswerStatus.UNCHECKED), anyCollection())).thenReturn(Optional.of(oldest));

        Mail mailed = digest.digestFor(user).orElseThrow();

        assertEquals("4টি এআই উত্তর আপনার যাচাইয়ের অপেক্ষায়; সবচেয়ে পুরনোটি 3 দিনের।", mailed.text());
        assertEquals("https://agrotrends.app/review", mailed.actionUrl());
    }

    @Test
    void aWeekAlreadySentIsNotSentAgain() {
        when(tagFollows.followedTagIds(7L)).thenReturn(List.of(3L));
        when(questions.unansweredWithTagsSince(any(), any(), any(), any())).thenReturn(List.of(question(5L, "Q")));
        when(sent.existsByUserIdAndWeekStart(eq(7L), any())).thenReturn(false, true);

        digest.sendWeekly();
        digest.sendWeekly();

        verify(sent).saveAndFlush(any(DigestSent.class));
        verify(mail).send(any(Mail.class));
    }
}
