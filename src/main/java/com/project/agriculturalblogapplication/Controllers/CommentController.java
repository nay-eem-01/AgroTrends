package com.project.agriculturalblogapplication.Controllers;

import com.project.agriculturalblogapplication.DTOS.CommentDto;
import com.project.agriculturalblogapplication.Service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app/blog/comment")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/addComment/user/{userId}/blog/{blogId}")
    public ResponseEntity<CommentDto> addComment(@RequestBody CommentDto commentDto, @PathVariable Long userId,  @PathVariable Long blogId){
        CommentDto newComment = commentService.addNewComment(commentDto,userId,blogId);
        return new ResponseEntity<>(newComment, HttpStatus.CREATED);
    }
}
