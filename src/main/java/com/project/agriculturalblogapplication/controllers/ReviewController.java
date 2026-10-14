package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.request.CorrectAiAnswerRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.QuestionAiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.ReviewItemResponse;
import com.project.agriculturalblogapplication.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Tag(name = "Review - controller", description = "Verified Experts check the AI's answers on questions in their topics.")
@RestController
@CommonApiResponses
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Unchecked AI answers in your expert topics, oldest first (Verified Experts only)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ReviewItemResponse.class)), responseCode = "200")
    @GetMapping("/api/review-queue")
    public ResponseEntity<HttpResponse> queue(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                              @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                              @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", reviewService.queue(pageNo, pageSize, lang));
    }

    @Operation(summary = "Confirm a question's AI answer as correct (Verified Expert in its topic)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = QuestionAiAnswerResponse.class)), responseCode = "200")
    @PostMapping("/api/questions/id/{questionId}/ai-answer/confirm")
    public ResponseEntity<HttpResponse> confirm(@PathVariable Long questionId,
                                                @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "AI answer confirmed.", reviewService.confirm(questionId, lang));
    }

    @Operation(summary = "Correct a question's AI answer: your answer is posted on the question and the AI answer "
            + "is marked CORRECTED (Verified Expert in its topic)", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = QuestionAiAnswerResponse.class)), responseCode = "200")
    @PostMapping("/api/questions/id/{questionId}/ai-answer/correct")
    public ResponseEntity<HttpResponse> correct(@PathVariable Long questionId, @Valid @RequestBody CorrectAiAnswerRequest request,
                                                @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "AI answer corrected.",
                reviewService.correct(questionId, request.getContent(), lang));
    }

    @Operation(summary = "Flag a question's AI answer as wrong or unsafe; it is hidden "
            + "(Verified Expert or Top Contributor in its topic)", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = QuestionAiAnswerResponse.class)), responseCode = "200")
    @PostMapping("/api/questions/id/{questionId}/ai-answer/flag")
    public ResponseEntity<HttpResponse> flag(@PathVariable Long questionId,
                                             @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "AI answer flagged.", reviewService.flag(questionId, lang));
    }

    @Operation(summary = "Admin: reopen a reviewed AI answer (back to UNCHECKED)", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = QuestionAiAnswerResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('EXPERT_REVIEW')")
    @PostMapping("/api/admin/questions/{questionId}/ai-answer/reopen")
    public ResponseEntity<HttpResponse> reopen(@PathVariable Long questionId,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "AI answer reopened.", reviewService.reopen(questionId, lang));
    }
}
