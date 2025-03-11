package com.project.agriculturalblogapplication.Service;

import com.project.agriculturalblogapplication.DTOS.CommentDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CommentService {
    CommentDto addNewComment(CommentDto commentDto,Long userId,Long BlogId);
    CommentDto replyToAComment(CommentDto commentDto);
    CommentDto updateComment(CommentDto commentDto);
    CommentDto deleteComment(CommentDto commentDto);
    List<CommentDto> viewAllComments();
    List<CommentDto> viewReplies();
}
