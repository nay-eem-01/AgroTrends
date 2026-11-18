package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.model.request.CreateAnswerRequest;
import com.project.agriculturalblogapplication.model.request.ReplyToAnswerRequest;
import com.project.agriculturalblogapplication.model.request.UpdateAnswerRequest;
import com.project.agriculturalblogapplication.model.response.AnswerResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.UserAnswerResponse;
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
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_SIZE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_SIZE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.LANG;

@Tag(name = "Answer controller", description = "Answer related operations.")
@RestController
@RequestMapping("/api/answers")
@CommonApiResponses
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "The asker accepts this answer (replaces an earlier accept)", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AnswerResponse.class)), responseCode = "200")
    @PutMapping(value = "/id/{answerId}/accept")
    public ResponseEntity<HttpResponse> accept(@PathVariable Long answerId,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Answer accepted.", answerService.accept(answerId, lang));
    }

    @Operation(summary = "The asker takes back accepting this answer", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AnswerResponse.class)), responseCode = "200")
    @DeleteMapping(value = "/id/{answerId}/accept")
    public ResponseEntity<HttpResponse> unaccept(@PathVariable Long answerId,
                                                 @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Accept removed.", answerService.unaccept(answerId, lang));
    }

    @Operation(summary = "Direct answers to a question: the accepted one first, then by score (replies via /replies)")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Answer.class))), responseCode = "200")
    @GetMapping(value = "/question/{questionId}")
    public ResponseEntity<HttpResponse> getAllByQuestionId(@PathVariable Long questionId) {
        return HttpResponse.getResponseEntity(
                true,
                "Answers loaded successfully.",
                answerService.getAllByQuestionId(questionId));
    }

    @Operation(summary = "A user's public answers for their profile, newest first, with the question title and whether it was accepted")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserAnswerResponse.class))), responseCode = "200")
    @GetMapping(value = "/all/user/{userId}")
    public ResponseEntity<HttpResponse> getAllByUser(@PathVariable Long userId,
                                                     @RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                     @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                     @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Answers loaded successfully.",
                answerService.getAllByUser(userId, pageNo, pageSize, lang));
    }

    @Operation(summary = "Get all replies by parent answer id")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Answer.class))), responseCode = "200")
    @GetMapping(value = "/replies/{parentAnswerId}")
    public ResponseEntity<HttpResponse> getRepliesByParentAnswerId(@PathVariable Long parentAnswerId) {
        return HttpResponse.getResponseEntity(
                true,
                "Replies loaded successfully.",
                answerService.viewReplies(parentAnswerId));
    }

    @Operation(summary = "Get answer by id")
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
    public ResponseEntity<HttpResponse> deleteAnswer(@PathVariable Long answerId,
                                                     @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        answerService.delete(answerId, lang);
        return HttpResponse.getResponseEntity(true, "Answer deleted successfully.");
    }
}