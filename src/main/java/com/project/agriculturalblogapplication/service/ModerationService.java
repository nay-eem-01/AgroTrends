package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Comment;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.Report;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.ReportResponse;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.notification.NotificationPublisher;
import com.project.agriculturalblogapplication.repositories.ReportRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.util.SafeHtml;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The moderators' report queue (roadmap 8.4). Removing content deletes it through the owners' own delete paths, so
 * it leaves every list, its AI vectors and the reputation it earned go with it; the reports keep a snapshot of what it
 * said. Reported AI answers are hidden (FLAGGED) instead, and users are banned, not removed.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    static final int SNAPSHOT_LENGTH = 2000;

    private final ReportRepository reportRepository;

    private final BlogService blogService;

    private final QuestionService questionService;

    private final AnswerService answerService;

    private final CommentService commentService;

    private final ReviewService reviewService;

    private final NotificationPublisher notificationPublisher;

    /** Reports in one status, oldest first. */
    @Transactional(readOnly = true)
    public Page<ReportResponse> reports(ReportStatus status, int pageNo, int pageSize) {
        return reportRepository.findAllByStatus(status,
                        CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "creationDate")))
                .map(ReportResponse::from);
    }

    @Transactional
    public ReportResponse dismiss(Long reportId, String lang) {
        Report report = open(reportId, lang);
        report.setStatus(ReportStatus.DISMISSED);
        return ReportResponse.from(report);
    }

    /** Removes the reported content and closes every open report on it; the author is told. */
    @Transactional
    public ReportResponse removeContent(Long reportId, String lang) {
        Report report = open(reportId, lang);
        Long id = report.getTargetId();
        // Snapshot and author are read before the delete; the delete comes last so nothing outlives a failure.
        Runnable delete;
        String snapshot;
        NotificationMessage notice;
        switch (report.getTargetType()) {
            case BLOG -> {
                Blog blog = blogService.findByIdWithException(id);
                snapshot = blog.getTitle() + "\n\n" + SafeHtml.plainText(blog.getContent());
                notice = removed(blog.getAuthor().getUser().getId(), NotificationTarget.BLOG, id);
                delete = () -> blogService.delete(id, lang);
            }
            case QUESTION -> {
                Question question = questionService.findByIdWithException(id);
                snapshot = question.getTitle() + "\n\n" + SafeHtml.plainText(question.getContent());
                notice = removed(question.getUser().getId(), NotificationTarget.QUESTION, id);
                delete = () -> questionService.delete(id, lang);
            }
            case ANSWER -> {
                Answer answer = answerService.findByIdWithException(id);
                snapshot = SafeHtml.plainText(answer.getContent());
                notice = removed(answer.getUser().getId(), NotificationTarget.QUESTION, answer.getQuestion().getId());
                delete = () -> answerService.delete(id, lang);
            }
            case COMMENT -> {
                Comment comment = commentService.findByIdWithException(id);
                snapshot = comment.getCommentContent();
                notice = removed(comment.getUser().getId(), NotificationTarget.BLOG, comment.getBlog().getId());
                delete = () -> commentService.delete(id, lang);
            }
            case AI_ANSWER -> {
                snapshot = reviewService.hide(id, lang);
                notice = null;
                delete = () -> { };
            }
            default -> throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_BAN_INSTEAD, lang);
        }
        reportRepository.closeAsActioned(report.getTargetType(), id, cut(snapshot));
        delete.run();
        if (notice != null) {
            notificationPublisher.publish(notice);
        }
        return ReportResponse.from(reportRepository.findById(reportId).orElseThrow());
    }

    private Report open(Long reportId, String lang) {
        Report report = reportRepository.findById(reportId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_REPORT_NOT_FOUND, lang));
        if (report.getStatus() != ReportStatus.OPEN) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_REPORT_ALREADY_CLOSED, lang);
        }
        return report;
    }

    // No actor: authors are not told which moderator acted.
    private static NotificationMessage removed(Long authorId, NotificationTarget target, Long targetId) {
        return new NotificationMessage(authorId, null, NotificationType.CONTENT_REMOVED, target, targetId);
    }

    private static String cut(String text) {
        return text == null || text.length() <= SNAPSHOT_LENGTH ? text : text.substring(0, SNAPSHOT_LENGTH);
    }
}
