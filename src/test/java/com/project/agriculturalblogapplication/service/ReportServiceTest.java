package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AppProperties;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Report;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.enums.ReportReason;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateReportRequest;
import com.project.agriculturalblogapplication.model.response.ReportResponse;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.ReportRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private static final String LANG = "en";
    private static final long REPORTER = 7L;

    private final ReportRepository repository = mock(ReportRepository.class);
    private final BlogService blogService = mock(BlogService.class);
    private final QuestionService questionService = mock(QuestionService.class);
    private final AnswerService answerService = mock(AnswerService.class);
    private final QuestionAiAnswerRepository aiAnswers = mock(QuestionAiAnswerRepository.class);
    private final CommentService commentService = mock(CommentService.class);
    private final UserService userService = mock(UserService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ModerationService moderation = mock(ModerationService.class);
    private final AppProperties appProperties = new AppProperties();
    private final ReportService reportService = new ReportService(repository, blogService, questionService, answerService,
            aiAnswers, commentService, userService, authorization, moderation, appProperties);

    @BeforeEach
    void setUp() {
        when(authorization.currentUserId(LANG)).thenReturn(REPORTER);
        when(userService.findByIdWithException(REPORTER, LANG)).thenReturn(new User());
        when(repository.save(any(Report.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void aReportIsOpenWithItsReasonAndChecksThatTheTargetExists() {
        ReportResponse response = reportService.report(request(ReportTarget.ANSWER, 5L, "  wrong dose "), LANG);

        assertEquals(ReportStatus.OPEN, response.status());
        assertEquals(ReportReason.HARMFUL_ADVICE, response.reason());
        assertEquals("wrong dose", response.details());
        verify(answerService).findByIdWithException(5L);
    }

    @Test
    void anAiAnswerIsFoundThroughItsQuestionAndBlankDetailsAreDropped() {
        when(aiAnswers.existsByQuestionIdAndStatusNotAndQuestionHiddenFalse(4L, AiAnswerStatus.FLAGGED)).thenReturn(true);

        assertNull(reportService.report(request(ReportTarget.AI_ANSWER, 4L, " "), LANG).details());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApplicationException.class,
                () -> reportService.report(request(ReportTarget.AI_ANSWER, 5L, null), LANG)).getHttpStatus());
    }

    @Test
    void aDraftCannotBeReportedSoItsExistenceIsNotConfirmed() {
        Blog draft = new Blog();
        draft.setStatus(BlogStatus.DRAFT);
        when(blogService.findByIdWithException(6L)).thenReturn(draft);

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApplicationException.class,
                () -> reportService.report(request(ReportTarget.BLOG, 6L, null), LANG)).getHttpStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void theThirdOpenReportHidesThePostButFewerDoNot() {
        when(repository.countByTargetTypeAndTargetIdAndStatus(ReportTarget.ANSWER, 5L, ReportStatus.OPEN)).thenReturn(2L, 3L);

        reportService.report(request(ReportTarget.ANSWER, 5L, null), LANG);
        verify(moderation, never()).hideForReview(any(), any());

        reportService.report(request(ReportTarget.ANSWER, 5L, null), LANG);
        verify(moderation).hideForReview(ReportTarget.ANSWER, 5L);
    }

    @Test
    void reportsNeverBanAUserAutomatically() {
        when(repository.countByTargetTypeAndTargetIdAndStatus(any(), any(), any())).thenReturn(99L);

        reportService.report(request(ReportTarget.USER, 30L, null), LANG);

        verify(moderation, never()).hideForReview(any(), any());
    }

    @Test
    void reportingTheSameTargetTwiceIsAConflict() {
        when(repository.existsByReporterIdAndTargetTypeAndTargetId(REPORTER, ReportTarget.QUESTION, 4L)).thenReturn(true);

        assertEquals(HttpStatus.CONFLICT, assertThrows(ApplicationException.class,
                () -> reportService.report(request(ReportTarget.QUESTION, 4L, null), LANG)).getHttpStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void aMissingTargetIsNotFoundAndNothingIsSaved() {
        when(commentService.findByIdWithException(9L))
                .thenThrow(new ApplicationException(HttpStatus.NOT_FOUND, "Comment not found"));

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApplicationException.class,
                () -> reportService.report(request(ReportTarget.COMMENT, 9L, null), LANG)).getHttpStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void nobodyCanReportThemselves() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApplicationException.class,
                () -> reportService.report(request(ReportTarget.USER, REPORTER, null), LANG)).getHttpStatus());
    }

    private static CreateReportRequest request(ReportTarget target, Long id, String details) {
        CreateReportRequest request = new CreateReportRequest();
        request.setTargetType(target);
        request.setTargetId(id);
        request.setReason(ReportReason.HARMFUL_ADVICE);
        request.setDetails(details);
        return request;
    }
}
