package com.project.agriculturalblogapplication.Service;

import com.project.agriculturalblogapplication.DTOS.CommentDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CommentService {
    CommentDto addNewComment(CommentDto commentDto,Long userId,Long BlogId);
    CommentDto replyToAComment(CommentDto commentDto, Long userId, Long blogId, Long parentCommentId);
    CommentDto updateComment(CommentDto commentDto, Long commentId);
    String deleteComment(Long  commentId);
    List<CommentDto> viewAllCommentsByBlogId(Long blogId);
    List<CommentDto> viewReplies(Long parentCommentId);


}
