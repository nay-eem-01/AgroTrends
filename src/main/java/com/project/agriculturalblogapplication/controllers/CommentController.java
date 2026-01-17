package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.dtos.CommentDto;
import com.project.agriculturalblogapplication.entities.Comment;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment controller", description = "Comment related operations.")
@RestController
@RequestMapping("/api/comments")
@CommonApiResponses
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Get all comments by blog id", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Comment.class))), responseCode = "200")
    @GetMapping(value = "/blog/{blogId}")
    public ResponseEntity<HttpResponse> getAllCommentsByBlogId(@PathVariable Long blogId) {
        return HttpResponse.getResponseEntity(
                true,
                "Comments loaded successfully.",
                commentService.viewAllCommentsByBlogId(blogId));
    }

    @Operation(summary = "Get all replies by parent comment id", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Comment.class))), responseCode = "200")
    @GetMapping(value = "/replies/{parentCommentId}")
    public ResponseEntity<HttpResponse> getRepliesByParentCommentId(@PathVariable Long parentCommentId) {
        return HttpResponse.getResponseEntity(
                true,
                "Replies loaded successfully.",
                commentService.viewReplies(parentCommentId));
    }

    @Operation(summary = "Get comment by id", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Comment.class)), responseCode = "200")
    @GetMapping(value = "/id/{commentId}")
    public ResponseEntity<HttpResponse> findById(@PathVariable Long commentId) {
        return HttpResponse.getResponseEntity(
                true,
                "Comment loaded successfully.",
                commentService.findByIdWithException(commentId));
    }

    @Operation(summary = "Add new comment", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Comment.class)), responseCode = "200")
    @PostMapping(value = "/blog/{blogId}/user/{userId}")
    public ResponseEntity<HttpResponse> addNewComment(
            @Valid @RequestBody CommentDto commentDto,
            @PathVariable Long userId,
            @PathVariable Long blogId,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {
        return HttpResponse.getResponseEntity(
                true,
                "Comment added successfully.",
                commentService.addNewComment(commentDto, userId, blogId, lang));
    }

    @Operation(summary = "Reply to a comment", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Comment.class)), responseCode = "200")
    @PostMapping(value = "/blog/{blogId}/user/{userId}/reply/{parentCommentId}")
    public ResponseEntity<HttpResponse> replyToComment(
            @Valid @RequestBody CommentDto commentDto,
            @PathVariable Long userId,
            @PathVariable Long blogId,
            @PathVariable Long parentCommentId,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {
        return HttpResponse.getResponseEntity(
                true,
                "Reply added successfully.",
                commentService.replyToAComment(commentDto, userId, blogId, parentCommentId, lang));
    }

    @Operation(summary = "Update comment", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Comment.class)), responseCode = "200")
    @PutMapping(value = "/id/{commentId}")
    public ResponseEntity<HttpResponse> updateComment(
            @Valid @RequestBody CommentDto commentDto,
            @PathVariable Long commentId) {
        return HttpResponse.getResponseEntity(
                true,
                "Comment updated successfully.",
                commentService.updateComment(commentDto, commentId));
    }

    @Operation(summary = "Delete comment", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @DeleteMapping(value = "/id/{commentId}")
    public ResponseEntity<HttpResponse> deleteComment(@PathVariable Long commentId) {
        commentService.delete(commentId);
        return HttpResponse.getResponseEntity(true, "Comment deleted successfully.");
    }
}