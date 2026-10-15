package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.Report;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.notification.NotificationPublisher;
import com.project.agriculturalblogapplication.repositories.ReportRepository;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ModerationServiceTest {

    private static final String LANG = "en";

    private final ReportRepository repository = mock(ReportRepository.class);
    private final BlogService blogService = mock(BlogService.class);
    private final QuestionService questionService = mock(QuestionService.class);
    private final AnswerService answerService = mock(AnswerService.class);
    private final CommentService commentService = mock(CommentService.class);
    private final ReviewService reviewService = mock(ReviewService.class);
    private final NotificationPublisher notifications = mock(NotificationPublisher.class);
    private final UserService userService = mock(UserService.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final ModerationService moderation = new ModerationService(repository, blogService, questionService,
            answerService, commentService, reviewService, notifications, userService, entityManager,
            () -> Optional.of("mod@agrotrends.test"));

    @Test
    void removingAnAnswerSnapshotsItClosesItsReportsDeletesItAndTellsTheAuthor() {
        report(ReportTarget.ANSWER, 8L, ReportStatus.OPEN);
        Question question = new Question();
        question.setId(4L);
        Answer answer = new Answer();
        answer.setUser(user(30L));
        answer.setQuestion(question);
        answer.setContent("<p>Spray <b>500 ml</b> per litre.</p>");
        when(entityManager.find(Answer.class, 8L)).thenReturn(answer);

        moderation.removeContent(1L, LANG);

        InOrder order = inOrder(repository, answerService, notifications);
        order.verify(repository).closeAsActioned(eq(ReportTarget.ANSWER), eq(8L), eq("Spray 500 ml per litre."),
                eq("mod@agrotrends.test"), any());
        order.verify(answerService).delete(8L, LANG);
        order.verify(notifications).publish(new NotificationMessage(30L, null, NotificationType.CONTENT_REMOVED,
                NotificationTarget.QUESTION, 4L));
    }

    @Test
    void aReportedAiAnswerIsHiddenNotDeletedAndNobodyIsNotified() {
        report(ReportTarget.AI_ANSWER, 4L, ReportStatus.OPEN);
        when(reviewService.hide(4L)).thenReturn("Spray 500 ml.");

        moderation.removeContent(1L, LANG);

        verify(repository).closeAsActioned(eq(ReportTarget.AI_ANSWER), eq(4L), eq("Spray 500 ml."), any(), any());
        verifyNoInteractions(notifications);
    }

    @Test
    void aUserCannotBeRemovedOnlyBanned() {
        report(ReportTarget.USER, 30L, ReportStatus.OPEN);

        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApplicationException.class, () -> moderation.removeContent(1L, LANG)).getHttpStatus());
        verify(repository, never()).closeAsActioned(any(), anyLong(), anyString(), any(), any());
    }

    @Test
    void banningSuspendsTheAccountAndClosesTheReportsAboutIt() {
        moderation.ban(30L, " Spam bot ", LANG);

        verify(userService).ban(30L, " Spam bot ", LANG);
        verify(repository).closeAsActioned(eq(ReportTarget.USER), eq(30L), eq("Banned: Spam bot"), eq("mod@agrotrends.test"), any());
    }

    @Test
    void aClosedReportCannotBeActedOnAgain() {
        report(ReportTarget.QUESTION, 4L, ReportStatus.DISMISSED);

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApplicationException.class, () -> moderation.removeContent(1L, LANG)).getHttpStatus());
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApplicationException.class, () -> moderation.dismiss(1L, LANG)).getHttpStatus());
        verify(questionService, never()).delete(anyLong(), anyString());
    }

    @Test
    void dismissingIsConditionalSoARemovalAtTheSameMomentWins() {
        report(ReportTarget.QUESTION, 4L, ReportStatus.OPEN);
        when(repository.dismissIfOpen(eq(1L), eq("mod@agrotrends.test"), any())).thenReturn(1);
        moderation.dismiss(1L, LANG);
        verify(questionService, never()).delete(anyLong(), anyString());

        // Regression: dismiss overwrote a concurrent ACTIONED close.
        when(repository.dismissIfOpen(eq(1L), any(), any())).thenReturn(0);
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApplicationException.class, () -> moderation.dismiss(1L, LANG)).getHttpStatus());
    }

    @Test
    void contentThatIsAlreadyGoneJustClosesTheReports() {
        // Regression: a report on an answer removed with its question could only be dismissed (404 on remove).
        report(ReportTarget.ANSWER, 8L, ReportStatus.OPEN);

        moderation.removeContent(1L, LANG);

        verify(repository).closeAsActioned(eq(ReportTarget.ANSWER), eq(8L), eq(ModerationService.ALREADY_GONE), any(), any());
        verify(answerService, never()).delete(anyLong(), anyString());
        verifyNoInteractions(notifications);
    }

    @Test
    void aLongSnapshotIsCut() {
        report(ReportTarget.QUESTION, 4L, ReportStatus.OPEN);
        Question question = new Question();
        question.setTitle("T");
        question.setContent("x".repeat(5000));
        question.setUser(user(30L));
        when(entityManager.find(Question.class, 4L)).thenReturn(question);

        moderation.removeContent(1L, LANG);

        verify(repository).closeAsActioned(eq(ReportTarget.QUESTION), eq(4L),
                eq(("T\n\n" + "x".repeat(5000)).substring(0, ModerationService.SNAPSHOT_LENGTH)), any(), any());
    }

    private Report report(ReportTarget target, Long targetId, ReportStatus status) {
        Report report = new Report();
        report.setId(1L);
        report.setReporter(user(7L));
        report.setTargetType(target);
        report.setTargetId(targetId);
        report.setStatus(status);
        when(repository.findById(1L)).thenReturn(Optional.of(report));
        return report;
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
