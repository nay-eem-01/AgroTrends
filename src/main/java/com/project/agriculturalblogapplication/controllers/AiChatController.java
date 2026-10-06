package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.request.AskQuestionRequest;
import com.project.agriculturalblogapplication.model.response.AiAnswerResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;

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
    public ResponseEntity<HttpResponse> ask(@RequestBody AskQuestionRequest request,
                                            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang
    ) {
        return HttpResponse.getResponseEntity(
                true,
                "Answer created successfully.",
                aiService.ask(request.question(), lang));
    }
}
