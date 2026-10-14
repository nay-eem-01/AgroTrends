package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.ReputationResponse;
import com.project.agriculturalblogapplication.service.ReputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reputation - controller", description = "Reputation earned from votes and accepted answers.")
@RestController
@CommonApiResponses
@RequiredArgsConstructor
public class ReputationController {

    private final ReputationService reputationService;

    @Operation(summary = "A user's reputation: total and per topic, highest first (public)")
    @ApiResponse(content = @Content(schema = @Schema(implementation = ReputationResponse.class)), responseCode = "200")
    @GetMapping("/api/users/{userId}/reputation")
    public ResponseEntity<HttpResponse> reputation(@PathVariable Long userId) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", reputationService.of(userId));
    }
}
