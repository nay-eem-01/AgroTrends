package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.model.request.CreateQuestionRequest;
import com.project.agriculturalblogapplication.model.request.UpdateQuestionRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.service.QuestionService;
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

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Tag(name = "Question controller", description = "Question related operations.")
@RestController
@RequestMapping("/api/questions")
@CommonApiResponses
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "Get all questions - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Question.class))), responseCode = "200")
    @GetMapping(value = "/all")
    public ResponseEntity<HttpResponse> getAll(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                               @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                               @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                               @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                questionService.getAll(paginationArgs));
    }

    @Operation(summary = "Get all questions by user - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Question.class))), responseCode = "200")
    @GetMapping(value = "/all/user/{userId}")
    public ResponseEntity<HttpResponse> getAllByUser(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                     @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                     @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                                     @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc,
                                                     @PathVariable Long userId,
                                                     @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                questionService.getAllByUser(paginationArgs, userId, lang));
    }

    @Operation(summary = "Get question info by id", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Question.class)), responseCode = "200")
    @GetMapping(value = "/id/{questionId}")
    public ResponseEntity<HttpResponse> findById(@PathVariable Long questionId) {
        return HttpResponse.getResponseEntity(
                true, "Data loaded successfully.", questionService.findById(questionId));
    }

    @Operation(summary = "New question creation", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Question.class)), responseCode = "200")
    @PostMapping(value = "/create")
    public ResponseEntity<HttpResponse> createNewQuestion(@Valid @RequestBody CreateQuestionRequest request,
                                                          @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {
        return HttpResponse.getResponseEntity(
                true, "Question created successfully.", questionService.create(request, lang));
    }

    @Operation(summary = "Update question info", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Question.class)), responseCode = "200")
    @PutMapping(value = "/update")
    public ResponseEntity<HttpResponse> updateQuestion(@Valid @RequestBody UpdateQuestionRequest request) {
        return HttpResponse.getResponseEntity(
                true, "Question updated successfully.", questionService.update(request));
    }

    @Operation(summary = "Delete question", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @DeleteMapping(value = "/id/{questionId}/delete")
    public ResponseEntity<HttpResponse> deleteQuestion(@PathVariable Long questionId) {
        questionService.delete(questionId);
        return HttpResponse.getResponseEntity(true, "Question deleted successfully.");
    }
}