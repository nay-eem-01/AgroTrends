package com.project.agriculturalblogapplication.Controllers.web;

import com.project.agriculturalblogapplication.DTOS.CommentDto;
import com.project.agriculturalblogapplication.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/blog/comment")
public class WebCommentController {
    private final CommentService commentService;

    public WebCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/addComment/user/{userId}/blog/{blogId}")
    public ResponseEntity<CommentDto> addComment(@RequestBody CommentDto commentDto, @PathVariable Long userId,  @PathVariable Long blogId){
        CommentDto newComment = commentService.addNewComment(commentDto,userId,blogId);
        return new ResponseEntity<>(newComment, HttpStatus.CREATED);
    }
    @GetMapping("/getComments/blog/{blogId}")
    public ResponseEntity<List<CommentDto>> getAllCommentsOnBlog(@PathVariable Long blogId){
        List<CommentDto> comments = commentService.viewAllCommentsByBlogId(blogId);
        return new ResponseEntity<>(comments, HttpStatus.OK);
    }

    @DeleteMapping("/deleteComment/commentId/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId){
        return new ResponseEntity<>(commentService.deleteComment(commentId),HttpStatus.OK);
    }

    @PutMapping("/updateComment/commentId/{commentId}")
    public ResponseEntity<CommentDto> updateComment(@RequestBody CommentDto commentDto, @PathVariable Long commentId){
        CommentDto updatedComment = commentService.updateComment(commentDto,commentId);

        return new ResponseEntity<>(updatedComment,HttpStatus.CREATED);
    }

    // Reply to a comment (nested)
    @PostMapping("/reply/user/{userId}/blog/{blogId}/parent/{parentCommentId}")
    public ResponseEntity<CommentDto> replyToComment(
            @RequestBody CommentDto commentDto,
            @PathVariable Long userId,
            @PathVariable Long blogId,
            @PathVariable Long parentCommentId) {

        CommentDto reply = commentService.replyToAComment(commentDto, userId, blogId, parentCommentId);
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

    // View all replies for a given comment
    @GetMapping("/replies/{parentCommentId}")
    public ResponseEntity<List<CommentDto>> viewReplies(@PathVariable Long parentCommentId) {
        List<CommentDto> replies = commentService.viewReplies(parentCommentId);
        return new ResponseEntity<>(replies, HttpStatus.OK);
    }

}
