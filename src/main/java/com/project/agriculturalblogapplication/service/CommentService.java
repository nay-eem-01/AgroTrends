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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final BlogService blogService;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    private final CommentRepository commentRepository;

    public CommentResponse create(CreateCommentRequest request, String lang) {
        Blog blog = blogService.findByIdWithException(request.getBlogId());
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
        Comment comment = findByIdWithException(commentId);
        authorizationService.assertOwnerOrAdmin(comment.getUser().getId(), lang);
        commentRepository.delete(comment);
    }

    public List<CommentResponse> getAllByBlogId(Long blogId) {
        blogService.findByIdWithException(blogId);

        List<Comment> comments = commentRepository.findByBlogIdAndParentCommentIsNull(blogId);

        return comments.stream()
                .map(this::mapToCommentResponse)
                .toList();
    }

    public CommentResponse reply(ReplyCommentRequest request, String lang) {
        Blog blog = blogService.findByIdWithException(request.getBlogId());

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

        return mapToCommentResponse(reply);
    }

    public List<CommentResponse> viewReplies(Long parentCommentId) {
        Comment parentComment = findByIdWithException(parentCommentId);

        return parentComment.getReplies().stream()
                .map(this::mapToCommentResponse)
                .toList();
    }

    public Comment findByIdWithException(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() ->
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