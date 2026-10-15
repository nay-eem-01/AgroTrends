package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Comment;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.Report;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import com.project.agriculturalblogapplication.events.AnswerChanged;
import com.project.agriculturalblogapplication.events.QuestionChanged;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.ReportResponse;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.notification.NotificationPublisher;
import com.project.agriculturalblogapplication.repositories.ReportRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.util.SafeHtml;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Moderation (roadmap 8.4, 8.5). Enough reports hide a post ({@link #hideForReview}); a moderator then confirms
 * them (the post is deleted through the owners' own delete paths, so its AI vectors and earned reputation go too) or
 * dismisses them (the post comes back). Reports keep a snapshot of what the post said. Users are banned, not removed.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    static final int SNAPSHOT_LENGTH = 2000;

    static final String ALREADY_GONE = "(already removed)";

    private final ReportRepository reportRepository;

    private final BlogService blogService;

    private final QuestionService questionService;

    private final AnswerService answerService;

    private final CommentService commentService;

    private final ReviewService reviewService;

    private final DocumentService documentService;

    private final NotificationPublisher notificationPublisher;

    private final ApplicationEventPublisher eventPublisher;

    private final UserService userService;

    private final EntityManager entityManager;

    private final AuditorAware<String> auditorAware;

    /** Reports in one status, oldest first. An open report with a snapshot is on a post that is hidden. */
    @Transactional(readOnly = true)
    public Page<ReportResponse> reports(ReportStatus status, int pageNo, int pageSize) {
        return reportRepository.findAllByStatus(status,
                        CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "creationDate")))
                .map(ReportResponse::from);
    }

    /**
     * Called when a post reaches the report threshold: hidden from everyone (and the AI) until a moderator decides,
     * its text kept on the open reports, its author told. Not an admin action, so it needs no privilege.
     */
    @Transactional
    public void hideForReview(ReportTarget type, Long id) {
        String snapshot;
        Post post = null;
        if (type == ReportTarget.AI_ANSWER) {
            snapshot = reviewService.hide(id);
        } else {
            post = load(type, id);
            snapshot = post != null && setHidden(type, id, post, true) ? post.snapshot() : null;
        }
        if (snapshot != null) {
            reportRepository.recordSnapshot(type, id, cut(snapshot));
            if (post != null) {
                notify(post, NotificationType.CONTENT_HIDDEN);
            }
        }
    }

    /**
     * The reports were unfounded: every open report on the post is dismissed and, if the threshold had hidden it,
     * the post is visible again and its author told.
     */
    @Transactional
    public ReportResponse dismiss(Long reportId, String lang) {
        Report report = open(reportId, lang);
        ReportTarget type = report.getTargetType();
        Long id = report.getTargetId();
        if (reportRepository.dismissAllOpen(type, id, moderator(), LocalDateTime.now()) == 0) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_REPORT_ALREADY_CLOSED, lang);
        }
        if (type == ReportTarget.AI_ANSWER) {
            reviewService.restore(id);
        } else {
            Post post = load(type, id);
            if (post != null && setHidden(type, id, post, false)) {
                notify(post, NotificationType.CONTENT_RESTORED);
            }
        }
        return ReportResponse.from(reportRepository.findById(reportId).orElseThrow());
    }

    /**
     * The reports are confirmed: the post is deleted for good (an AI answer is hidden instead) and every open report
     * on it closes as ACTIONED with its snapshot. A post that is already gone just closes the reports.
     */
    @Transactional
    public ReportResponse removeContent(Long reportId, String lang) {
        Report report = open(reportId, lang);
        ReportTarget type = report.getTargetType();
        Long id = report.getTargetId();
        if (type == ReportTarget.USER) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_BAN_INSTEAD, lang);
        }
        if (type == ReportTarget.AI_ANSWER) {
            String hidden = reviewService.hide(id);
            reportRepository.closeAsActioned(type, id, cut(hidden == null ? ALREADY_GONE : hidden), moderator(),
                    LocalDateTime.now());
            return ReportResponse.from(reportRepository.findById(reportId).orElseThrow());
        }
        Post post = load(type, id);
        // The delete comes last, so nothing outlives a failure.
        reportRepository.closeAsActioned(type, id, cut(post == null ? ALREADY_GONE : post.snapshot()), moderator(),
                LocalDateTime.now());
        if (post != null) {
            switch (type) {
                case BLOG -> blogService.removeForModeration(id);
                case QUESTION -> questionService.removeForModeration(id);
                case ANSWER -> answerService.removeForModeration(id);
                default -> commentService.removeForModeration(id);
            }
            notify(post, NotificationType.CONTENT_REMOVED);
        }
        return ReportResponse.from(reportRepository.findById(reportId).orElseThrow());
    }

    /** Suspends the account, signs it out everywhere and closes the open reports about them. */
    @Transactional
    public void ban(Long userId, String reason, String lang) {
        userService.ban(userId, reason, lang);
        reportRepository.closeAsActioned(ReportTarget.USER, userId, "Banned: " + reason.strip(), moderator(),
                LocalDateTime.now());
    }

    @Transactional
    public void unban(Long userId, String lang) {
        userService.unban(userId, lang);
    }

    /** What moderation needs to know about a reported post; null when it no longer exists. */
    private record Post(Object entity, String snapshot, Long authorId, NotificationTarget link, Long linkId) {
    }

    // Looked up with find (null, not an exception): an exception out of another transactional service would mark this
    // transaction rollback-only. Hidden posts are found too, unlike through the services.
    private Post load(ReportTarget type, Long id) {
        return switch (type) {
            case BLOG -> {
                Blog blog = entityManager.find(Blog.class, id);
                yield blog == null ? null : new Post(blog, blog.getTitle() + "\n\n" + SafeHtml.plainText(blog.getContent()),
                        blog.getAuthor().getUser().getId(), NotificationTarget.BLOG, id);
            }
            case QUESTION -> {
                Question question = entityManager.find(Question.class, id);
                yield question == null ? null : new Post(question,
                        question.getTitle() + "\n\n" + SafeHtml.plainText(question.getContent()),
                        question.getUser().getId(), NotificationTarget.QUESTION, id);
            }
            case ANSWER -> {
                Answer answer = entityManager.find(Answer.class, id);
                yield answer == null ? null : new Post(answer, SafeHtml.plainText(answer.getContent()),
                        answer.getUser().getId(), NotificationTarget.QUESTION, answer.getQuestion().getId());
            }
            case COMMENT -> {
                Comment comment = entityManager.find(Comment.class, id);
                yield comment == null ? null : new Post(comment, comment.getCommentContent(),
                        comment.getUser().getId(), NotificationTarget.BLOG, comment.getBlog().getId());
            }
            // AI answers are handled through ReviewService; users are banned, not loaded.
            case AI_ANSWER, USER -> null;
        };
    }

    /**
     * Hides or shows a post; false when it already was in that state. A blog leaves or rejoins the vector store at
     * once; questions and answers publish an event so the AI index follows after the commit.
     */
    private boolean setHidden(ReportTarget type, Long id, Post post, boolean hidden) {
        switch (post.entity()) {
            case Blog blog -> {
                if (blog.getStatus() != (hidden ? BlogStatus.PUBLISHED : BlogStatus.HIDDEN)) {
                    return false;
                }
                blog.setStatus(hidden ? BlogStatus.HIDDEN : BlogStatus.PUBLISHED);
                if (hidden) {
                    documentService.deleteBlog(id);
                } else {
                    documentService.indexBlog(blog);
                }
            }
            case Question question -> {
                if (question.isHidden() == hidden) {
                    return false;
                }
                question.setHidden(hidden);
                eventPublisher.publishEvent(new QuestionChanged(id));
            }
            case Answer answer -> {
                if (answer.isHidden() == hidden) {
                    return false;
                }
                answer.setHidden(hidden);
                eventPublisher.publishEvent(new AnswerChanged(id));
            }
            case Comment comment -> {
                if (comment.isHidden() == hidden) {
                    return false;
                }
                comment.setHidden(hidden);
            }
            default -> throw new IllegalStateException("Not a post: " + type);
        }
        return true;
    }

    // No actor: authors are not told which moderator acted.
    private void notify(Post post, NotificationType type) {
        notificationPublisher.publish(new NotificationMessage(post.authorId(), null, type, post.link(), post.linkId()));
    }

    private Report open(Long reportId, String lang) {
        Report report = reportRepository.findById(reportId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_REPORT_NOT_FOUND, lang));
        if (report.getStatus() != ReportStatus.OPEN) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_REPORT_ALREADY_CLOSED, lang);
        }
        return report;
    }

    private String moderator() {
        return auditorAware.getCurrentAuditor().orElse("SYSTEM");
    }

    private static String cut(String text) {
        return text == null || text.length() <= SNAPSHOT_LENGTH ? text : text.substring(0, SNAPSHOT_LENGTH);
    }
}
