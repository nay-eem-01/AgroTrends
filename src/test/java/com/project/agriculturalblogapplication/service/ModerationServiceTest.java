package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.events.QuestionChanged;
import org.springframework.context.ApplicationEventPublisher;
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
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private final DocumentService documentService = mock(DocumentService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ModerationService moderation = new ModerationService(repository, blogService, questionService,
            answerService, commentService, reviewService, documentService, notifications, events, userService,
            entityManager, () -> Optional.of("mod@agrotrends.test"), authorization);

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
        order.verify(answerService).removeForModeration(8L);
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
        verify(questionService, never()).removeForModeration(anyLong());
    }

    @Test
    void reachingTheThresholdHidesAQuestionKeepsItsTextAndTellsTheAuthor() {
        Question question = question(4L, 30L);
        when(entityManager.find(Question.class, 4L)).thenReturn(question);

        moderation.hideForReview(ReportTarget.QUESTION, 4L);

        assertTrue(question.isHidden());
        verify(events).publishEvent(new QuestionChanged(4L));
        verify(repository).recordSnapshot(ReportTarget.QUESTION, 4L, "Why?\n\nLeaves are yellow");
        verify(notifications).publish(new NotificationMessage(30L, null, NotificationType.CONTENT_HIDDEN,
                NotificationTarget.QUESTION, 4L));
    }

    @Test
    void anAuthorWhoseOwnReportTippedTheThresholdIsNotNotified() {
        when(entityManager.find(Question.class, 4L)).thenReturn(question(4L, 30L));
        when(authorization.signedInUserId()).thenReturn(Optional.of(30L));

        moderation.hideForReview(ReportTarget.QUESTION, 4L);

        verifyNoInteractions(notifications);
    }

    @Test
    void anAlreadyHiddenPostIsNotHiddenOrAnnouncedAgain() {
        Question question = question(4L, 30L);
        question.setHidden(true);
        when(entityManager.find(Question.class, 4L)).thenReturn(question);

        moderation.hideForReview(ReportTarget.QUESTION, 4L);

        verifyNoInteractions(events, notifications);
        verify(repository, never()).recordSnapshot(any(), anyLong(), anyString());
    }

    @Test
    void aHiddenBlogLeavesTheAiIndexAndComesBackWhenTheReportsAreDismissed() {
        Blog blog = new Blog();
        blog.setTitle("Fake cure");
        blog.setContent("Buy now");
        blog.setStatus(BlogStatus.PUBLISHED);
        Author author = new Author();
        author.setUser(user(30L));
        blog.setAuthor(author);
        when(entityManager.find(Blog.class, 6L)).thenReturn(blog);

        moderation.hideForReview(ReportTarget.BLOG, 6L);
        assertEquals(BlogStatus.HIDDEN, blog.getStatus());
        verify(documentService).deleteBlog(6L);

        report(ReportTarget.BLOG, 6L, ReportStatus.OPEN);
        when(repository.dismissAllOpen(eq(ReportTarget.BLOG), eq(6L), eq("mod@agrotrends.test"), any())).thenReturn(3);
        moderation.dismiss(1L, LANG);

        assertEquals(BlogStatus.PUBLISHED, blog.getStatus());
        verify(documentService).indexBlog(blog);
        verify(notifications).publish(new NotificationMessage(30L, null, NotificationType.CONTENT_RESTORED,
                NotificationTarget.BLOG, 6L));
    }

    @Test
    void dismissingAPostThatWasNeverHiddenClosesItsReportsQuietly() {
        report(ReportTarget.QUESTION, 4L, ReportStatus.OPEN);
        when(repository.dismissAllOpen(eq(ReportTarget.QUESTION), eq(4L), any(), any())).thenReturn(1);
        when(entityManager.find(Question.class, 4L)).thenReturn(question(4L, 30L));

        moderation.dismiss(1L, LANG);

        verifyNoInteractions(events, notifications);
        verify(questionService, never()).removeForModeration(anyLong());
    }

    @Test
    void dismissingAfterAnotherModeratorActedIsAConflict() {
        // Regression: dismiss overwrote a concurrent ACTIONED close.
        report(ReportTarget.QUESTION, 4L, ReportStatus.OPEN);
        when(repository.dismissAllOpen(eq(ReportTarget.QUESTION), eq(4L), any(), any())).thenReturn(0);

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApplicationException.class, () -> moderation.dismiss(1L, LANG)).getHttpStatus());
        verifyNoInteractions(notifications);
    }

    @Test
    void dismissedReportsOnAnAiAnswerAskReviewToRestoreIt() {
        report(ReportTarget.AI_ANSWER, 4L, ReportStatus.OPEN);
        when(repository.dismissAllOpen(eq(ReportTarget.AI_ANSWER), eq(4L), any(), any())).thenReturn(2);

        moderation.dismiss(1L, LANG);

        verify(reviewService).restore(4L);
    }

    @Test
    void contentThatIsAlreadyGoneJustClosesTheReports() {
        // Regression: a report on an answer removed with its question could only be dismissed (404 on remove).
        report(ReportTarget.ANSWER, 8L, ReportStatus.OPEN);

        moderation.removeContent(1L, LANG);

        verify(repository).closeAsActioned(eq(ReportTarget.ANSWER), eq(8L), eq(ModerationService.ALREADY_GONE), any(), any());
        verify(answerService, never()).removeForModeration(anyLong());
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

    private static Question question(Long id, Long authorId) {
        Question question = new Question();
        question.setId(id);
        question.setTitle("Why?");
        question.setContent("<p>Leaves are yellow</p>");
        question.setUser(user(authorId));
        return question;
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
