package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AppProperties;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.enums.MailKind;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.mail.Mail;
import com.project.agriculturalblogapplication.mail.MailService;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.security.service.UnsubscribeTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** E-mail the asker (frontend request B8). */
class AnswerMailServiceTest {

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final QuestionAiAnswerRepository aiAnswers = mock(QuestionAiAnswerRepository.class);
    private final MailService mail = mock(MailService.class);
    private final UnsubscribeTokens tokens = new UnsubscribeTokens("a-test-secret-that-is-at-least-32-characters");
    private AnswerMailService service;

    private Question question;
    private User asker;
    private final LocalDateTime asked = LocalDateTime.now().minusHours(5);

    @BeforeEach
    void setUp() {
        AppProperties app = new AppProperties();
        app.setBackendUrl("https://api.agrotrends.app");
        service = new AnswerMailService(questions, answers, aiAnswers, mail, tokens, app,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
        ReflectionTestUtils.setField(service, "frontendUrl", "https://agrotrends.app");
        asker = new User();
        asker.setId(7L);
        asker.setEmail("asker@example.com");
        asker.setEmailVerifiedAt(LocalDateTime.now());
        asker.setPreferredLang("en");
        question = new Question();
        question.setId(5L);
        question.setTitle("Yellow rice leaves");
        question.setUser(asker);
        question.setCreationDate(asked);
        when(questions.findById(5L)).thenReturn(Optional.of(question));
        when(aiAnswers.findByQuestionId(5L)).thenReturn(Optional.empty());
    }

    private Answer answer(String name, String html) {
        User author = new User();
        author.setName(name);
        Answer answer = new Answer();
        answer.setUser(author);
        answer.setContent(html);
        return answer;
    }

    @Test
    void oneAnswerIsMailedWithItsAuthorTextLinkAndUnsubscribe() {
        when(answers.newAnswersSince(5L, asked)).thenReturn(List.of(answer("Karim", "<p>Add <b>potash</b>.</p>")));

        Mail sent = service.mailFor(question, asked).orElseThrow();

        assertEquals("asker@example.com", sent.to());
        assertEquals("Yellow rice leaves", sent.subject());
        assertEquals("Karim answered your question:\n\nAdd potash.", sent.text());
        assertEquals("https://agrotrends.app/questions/5", sent.actionUrl());
        assertTrue(sent.unsubscribeUrl().startsWith("https://api.agrotrends.app/api/user/unsubscribe?token="
                + tokens.tokenFor(7L, MailKind.answer)));
        assertTrue(sent.unsubscribeUrl().endsWith("&kind=answer"));
    }

    @Test
    void severalAnswersAndAnExpertCheckFoldIntoOneBanglaMail() {
        asker.setPreferredLang("bn");
        when(answers.newAnswersSince(5L, asked)).thenReturn(List.of(answer("A", "x"), answer("B", "y")));
        QuestionAiAnswer ai = new QuestionAiAnswer();
        ai.setStatus(AiAnswerStatus.CORRECTED);
        ai.setReviewedAt(asked.plusHours(1));
        when(aiAnswers.findByQuestionId(5L)).thenReturn(Optional.of(ai));

        Mail sent = service.mailFor(question, asked).orElseThrow();

        assertEquals("আপনার প্রশ্নে 2টি নতুন উত্তর এসেছে।\n\nএকজন বিশেষজ্ঞ এআই উত্তরটি সংশোধন করেছেন।", sent.text());
        assertEquals("উত্তর দেখুন", sent.actionLabel());
    }

    @Test
    void noMailWithoutAConfirmedAddressOrConsentOrAnythingNew() {
        when(answers.newAnswersSince(5L, asked)).thenReturn(List.of(answer("Karim", "x")));
        asker.setEmailVerifiedAt(null);
        assertTrue(service.mailFor(question, asked).isEmpty());

        asker.setEmailVerifiedAt(LocalDateTime.now());
        asker.setEmailOnAnswer(false);
        assertTrue(service.mailFor(question, asked).isEmpty());

        asker.setEmailOnAnswer(true);
        when(answers.newAnswersSince(5L, asked)).thenReturn(List.of());
        assertTrue(service.mailFor(question, asked).isEmpty());
    }

    @Test
    void withinTheHourNothingIsSentAndTheSchedulerSendsItLater() {
        question.setAnswerMailedAt(LocalDateTime.now().minusMinutes(20));

        service.onNotification(new NotificationMessage(7L, 8L, NotificationType.ANSWER_ON_YOUR_QUESTION,
                NotificationTarget.QUESTION, 5L));

        verify(questions).markAnswerMailPending(5L);
        verify(questions, never()).claimNextAnswerMail(eq(5L), any(), any());
        verify(mail, never()).send(any());
    }

    @Test
    void theFirstAnswerIsSentAtOnceWhenTheClaimIsOurs() {
        when(questions.claimFirstAnswerMail(eq(5L), any())).thenReturn(1);
        when(answers.newAnswersSince(5L, asked)).thenReturn(List.of(answer("Karim", "x")));

        service.onNotification(new NotificationMessage(7L, 8L, NotificationType.ANSWER_ON_YOUR_QUESTION,
                NotificationTarget.QUESTION, 5L));

        verify(mail).send(any(Mail.class));
    }

    @Test
    void otherNotificationsSendNothing() {
        service.onNotification(new NotificationMessage(7L, 8L, NotificationType.COMMENT_REPLY, NotificationTarget.BLOG, 5L));

        verify(questions, never()).markAnswerMailPending(any());
    }
}
