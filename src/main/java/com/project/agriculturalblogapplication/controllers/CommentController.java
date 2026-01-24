package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.Comment;
import com.project.agriculturalblogapplication.model.request.CreateCommentRequest;
import com.project.agriculturalblogapplication.model.request.ReplyCommentRequest;
import com.project.agriculturalblogapplication.model.request.UpdateCommentRequest;
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

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;

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
                commentService.getAllByBlogId(blogId));
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
                commentService.findById(commentId));
    }

    @Operation(summary = "Add new comment", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Comment.class)), responseCode = "200")
    @PostMapping(value = "/create")
    public ResponseEntity<HttpResponse> create(
            @Valid @RequestBody CreateCommentRequest request,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(
                true,
                "Comment added successfully.",
                commentService.create(request, lang));
    }

    @Operation(summary = "Reply to a comment", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Comment.class)), responseCode = "200")
    @PostMapping(value = "/reply")
    public ResponseEntity<HttpResponse> replyToComment(
            @Valid @RequestBody ReplyCommentRequest request,
            @RequestParam(value = "Accept-Language", defaultValue = "en") String lang) {

        return HttpResponse.getResponseEntity(
                true,
                "Reply added successfully.",
                commentService.reply(request, lang));
    }

    @Operation(summary = "Update comment", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Comment.class)), responseCode = "200")
    @PutMapping(value = "/update")
    public ResponseEntity<HttpResponse> update(
            @Valid @RequestBody UpdateCommentRequest request,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {

        return HttpResponse.getResponseEntity(
                true,
                "Comment updated successfully.",
                commentService.update(request, lang));
    }

    @Operation(summary = "Delete comment", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @DeleteMapping(value = "/id/{commentId}")
    public ResponseEntity<HttpResponse> delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
        return HttpResponse.getResponseEntity(true, "Comment deleted successfully.");
    }
}