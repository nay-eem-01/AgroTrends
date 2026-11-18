package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.PublicStatsResponse;
import com.project.agriculturalblogapplication.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stats - controller", description = "Public headline numbers.")
@RestController
@CommonApiResponses
@RequiredArgsConstructor
public class StatsController {

    private final AdminService adminService;

    @Operation(summary = "Members, verified experts, questions and answered questions (public, refreshed hourly)")
    @ApiResponse(content = @Content(schema = @Schema(implementation = PublicStatsResponse.class)), responseCode = "200")
    @GetMapping("/api/stats/public")
    public ResponseEntity<HttpResponse> publicStats() {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", adminService.publicStats());
    }
}
