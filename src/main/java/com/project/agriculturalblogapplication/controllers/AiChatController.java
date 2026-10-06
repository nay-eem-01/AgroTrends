package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.request.AskQuestionRequest;
import com.project.agriculturalblogapplication.model.response.AiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.AiHistoryItemResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
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
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_SIZE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_SIZE;

@Tag(name = "AI Chat - controller", description = "AI chat related operations.")
@RestController
@RequestMapping("/api/ai")
@CommonApiResponses
@RequiredArgsConstructor
public class AiChatController {

    private final AiService aiService;

    @Operation(summary = "Ask the AI advisor; the answer is grounded in matching AgroTrends posts, which are returned as sources",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AiAnswerResponse.class)), responseCode = "200")
    @PostMapping(value = "/ask")
    public ResponseEntity<HttpResponse> ask(@Valid @RequestBody AskQuestionRequest request,
                                            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang
    ) {
        return HttpResponse.getResponseEntity(
                true,
                "Answer created successfully.",
                aiService.ask(request.question(), lang));
    }

    @Operation(summary = "Your own AI questions and answers, newest first", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AiHistoryItemResponse.class)), responseCode = "200")
    @GetMapping(value = "/history")
    public ResponseEntity<HttpResponse> history(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                aiService.history(pageNo, pageSize, lang));
    }
}
