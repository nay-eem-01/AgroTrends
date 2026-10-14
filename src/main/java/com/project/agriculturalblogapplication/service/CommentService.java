package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Comment;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.CreateCommentRequest;
import com.project.agriculturalblogapplication.model.request.ReplyCommentRequest;
import com.project.agriculturalblogapplication.model.request.UpdateCommentRequest;
import com.project.agriculturalblogapplication.model.response.CommentResponse;
import com.project.agriculturalblogapplication.repositories.CommentRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.notification.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Comment's user, blog and parent are lazy: every method maps them inside a transaction (open-in-view is off).
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final BlogService blogService;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    private final CommentRepository commentRepository;

    private final NotificationPublisher notificationPublisher;

    public CommentResponse create(CreateCommentRequest request, String lang) {
        Blog blog = blogService.findVisibleBlog(request.getBlogId(), lang);
        User user = userService.findByIdWithException(authorizationService.currentUserId(lang), lang);

        Comment comment = new Comment();
        comment.setBlog(blog);
        comment.setUser(user);
        comment.setCommentContent(request.getContent());
        comment =  commentRepository.save(comment);

        return mapToCommentResponse(comment);
    }

    public CommentResponse update(UpdateCommentRequest request, String lang) {
        Comment comment = findByIdWithException(request.getCommentId());

        authorizationService.assertOwnerOrAdmin(comment.getUser().getId(), lang);

        comment.setCommentContent(request.getContent());
        comment = commentRepository.save(comment);

        return mapToCommentResponse(comment);
    }

    public void delete(Long commentId, String lang) {
        // Hidden comments can still be deleted, by their author or a moderator confirming the reports.
        Comment comment = commentRepository.findById(commentId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_COMMENT_NOT_FOUND));
        authorizationService.assertOwnerAdminOrModerator(comment.getUser().getId(), lang);
        commentRepository.delete(comment);
    }

    public List<CommentResponse> getAllByBlogId(Long blogId, String lang) {
        blogService.findVisibleBlog(blogId, lang);

        List<Comment> comments = commentRepository.findByBlogIdAndParentCommentIsNullAndHiddenFalse(blogId);

        return comments.stream()
                .map(this::mapToCommentResponse)
                .toList();
    }

    public CommentResponse reply(ReplyCommentRequest request, String lang) {
        Blog blog = blogService.findVisibleBlog(request.getBlogId(), lang);

        User user = userService.findByIdWithException(authorizationService.currentUserId(lang), lang);

        Comment parentComment = findByIdWithException(request.getParentCommentId());

        if (!parentComment.getBlog().getId().equals(request.getBlogId())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_COMMENT_BLOG_MISMATCH);
        }

        Comment reply = new Comment();
        reply.setUser(user);
        reply.setBlog(blog);
        reply.setParentComment(parentComment);
        reply.setCommentContent(request.getContent());

        reply =  commentRepository.save(reply);
        notificationPublisher.publish(new NotificationMessage(parentComment.getUser().getId(), user.getId(),
                NotificationType.COMMENT_REPLY, NotificationTarget.BLOG, blog.getId()));

        return mapToCommentResponse(reply);
    }

    public List<CommentResponse> viewReplies(Long parentCommentId) {
        Comment parentComment = findByIdWithException(parentCommentId);

        return parentComment.getReplies().stream()
                .filter(reply -> !reply.isHidden())
                .map(this::mapToCommentResponse)
                .toList();
    }

    /** A hidden comment is a 404 here: it cannot be read, edited or replied to while under review. */
    public Comment findByIdWithException(Long commentId) {
        return commentRepository.findById(commentId).filter(comment -> !comment.isHidden()).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_COMMENT_NOT_FOUND));
    }

    public CommentResponse findById(Long commentId) {
        return mapToCommentResponse(findByIdWithException(commentId));
    }

    private CommentResponse mapToCommentResponse(Comment comment){
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getId());

        // Top-level comments have no parent.
        response.setParentCommentId(comment.getParentComment() == null ? null : comment.getParentComment().getId());
        response.setBlogId(comment.getBlog().getId());
        response.setUserId(comment.getUser().getId());
        response.setContent(comment.getCommentContent());
        response.setAuthorName(comment.getUser().getName());
        response.setCreatedAt(CommonUtils.toInstant(comment.getCreationDate()));
        response.setUpdatedAt(CommonUtils.toInstant(comment.getLastModifiedDate()));

        return response;
    }
}