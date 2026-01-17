package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.dtos.CommentDto;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Comment;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.repositories.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final BlogService blogService;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final ModelMapper modelMapper;

    public Comment addNewComment(CommentDto commentDto, Long userId, Long blogId, String lang) {
        Blog blog = blogService.findByIdWithException(blogId);
        User user = userService.findByIdWithException(userId, lang);

        Comment newComment = modelMapper.map(commentDto, Comment.class);
        newComment.setUser(user);
        newComment.setBlog(blog);

        return commentRepository.save(newComment);

    }

    public Comment updateComment(CommentDto commentDto, Long commentId) {
        Comment comment = findByIdWithException(commentId);
        comment.setCommentContent(commentDto.getCommentContent());

        return commentRepository.save(comment);
    }

    public void delete(Long commentId) {
        Comment comment = findByIdWithException(commentId);
        commentRepository.delete(comment);
    }

    public List<Comment> viewAllCommentsByBlogId(Long blogId) {
        blogService.findByIdWithException(blogId);

        return commentRepository.findByBlogIdAndParentCommentIsNull(blogId);
    }

    public Comment replyToAComment(CommentDto commentDto, Long userId, Long blogId, Long parentCommentId, String lang) {
        Blog blog = blogService.findByIdWithException(blogId);
        User user = userService.findByIdWithException(userId, lang);
        Comment parentComment = findByIdWithException(parentCommentId);

        if (!parentComment.getBlog().getId().equals(blogId)) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_COMMENT_BLOG_MISMATCH);
        }

        Comment reply = modelMapper.map(commentDto, Comment.class);
        reply.setUser(user);
        reply.setBlog(blog);
        reply.setParentComment(parentComment);

        return commentRepository.save(reply);
    }

    public List<Comment> viewReplies(Long parentCommentId) {
        Comment parentComment = findByIdWithException(parentCommentId);
        return parentComment.getReplies();
    }

    public Comment findByIdWithException(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_COMMENT_NOT_FOUND));
    }
}