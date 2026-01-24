package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.model.request.CreateAnswerRequest;
import com.project.agriculturalblogapplication.model.request.ReplyToAnswerRequest;
import com.project.agriculturalblogapplication.model.request.UpdateAnswerRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.AnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;

@Tag(name = "Answer controller", description = "Answer related operations.")
@RestController
@RequestMapping("/api/answers")
@CommonApiResponses
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "Get all answers by question id", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Answer.class))), responseCode = "200")
    @GetMapping(value = "/question/{questionId}")
    public ResponseEntity<HttpResponse> getAllByQuestionId(@PathVariable Long questionId) {
        return HttpResponse.getResponseEntity(
                true,
                "Answers loaded successfully.",
                answerService.getAllByQuestionId(questionId));
    }

    @Operation(summary = "Get all replies by parent answer id", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Answer.class))), responseCode = "200")
    @GetMapping(value = "/replies/{parentAnswerId}")
    public ResponseEntity<HttpResponse> getRepliesByParentAnswerId(@PathVariable Long parentAnswerId) {
        return HttpResponse.getResponseEntity(
                true,
                "Replies loaded successfully.",
                answerService.viewReplies(parentAnswerId));
    }

    @Operation(summary = "Get answer by id", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Answer.class)), responseCode = "200")
    @GetMapping(value = "/id/{answerId}")
    public ResponseEntity<HttpResponse> findById(@PathVariable Long answerId) {
        return HttpResponse.getResponseEntity(
                true,
                "Answer loaded successfully.",
                answerService.findById(answerId));
    }

    @Operation(summary = "Add new answer", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Answer.class)), responseCode = "200")
    @PostMapping(value = "/create")
    public ResponseEntity<HttpResponse> create(
            @Valid @RequestBody CreateAnswerRequest request,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang
            ) {

        return HttpResponse.getResponseEntity(
                true,
                "Answer created successfully.",
                answerService.create(request, lang));
    }

    @Operation(summary = "Reply to an answer", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Answer.class)), responseCode = "200")
    @PostMapping(value = "/reply")
    public ResponseEntity<HttpResponse> replyToAnswer(
            @Valid @RequestBody ReplyToAnswerRequest request,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {

        return HttpResponse.getResponseEntity(
                true,
                "Reply added successfully.",
                answerService.replyToAnswer(request, lang));
    }

    @Operation(summary = "Update answer", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Answer.class)), responseCode = "200")
    @PutMapping(value = "/update/")
    public ResponseEntity<HttpResponse> updateAnswer(
            @Valid @RequestBody UpdateAnswerRequest request,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang
        ) {
        return HttpResponse.getResponseEntity(
                true,
                "Answer updated successfully.",
                answerService.update(request, lang));
    }

    @Operation(summary = "Delete answer", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @DeleteMapping(value = "/delete/id/{answerId}")
    public ResponseEntity<HttpResponse> deleteAnswer(@PathVariable Long answerId) {
        answerService.delete(answerId);
        return HttpResponse.getResponseEntity(true, "Answer deleted successfully.");
    }
}