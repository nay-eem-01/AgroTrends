package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.model.request.BanRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.ReportResponse;
import com.project.agriculturalblogapplication.service.ModerationService;
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

@Tag(name = "Moderation - controller", description = "Admin: the report queue, acting on reports, and bans.")
@RestController
@CommonApiResponses
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CONTENT_MODERATE')")
public class ModerationController {

    private final ModerationService moderationService;

    @Operation(summary = "Admin: reports in one status (default OPEN), oldest first", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ReportResponse.class)), responseCode = "200")
    @GetMapping("/api/admin/reports")
    public ResponseEntity<HttpResponse> reports(@RequestParam(name = "status", defaultValue = "OPEN") ReportStatus status,
                                                @RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", moderationService.reports(status, pageNo, pageSize));
    }

    @Operation(summary = "Admin: close a report without action", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ReportResponse.class)), responseCode = "200")
    @PostMapping("/api/admin/reports/{reportId}/dismiss")
    public ResponseEntity<HttpResponse> dismiss(@PathVariable Long reportId,
                                                @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Report dismissed.", moderationService.dismiss(reportId, lang));
    }

    @Operation(summary = "Admin: remove the reported post for good (an AI answer is hidden instead); closes every open "
            + "report on it and tells the author", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ReportResponse.class)), responseCode = "200")
    @PostMapping("/api/admin/reports/{reportId}/remove-content")
    public ResponseEntity<HttpResponse> removeContent(@PathVariable Long reportId,
                                                      @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Content removed.", moderationService.removeContent(reportId, lang));
    }

    @Operation(summary = "Admin: ban a user: sign-in and token refresh are refused and every session ends; closes the "
            + "open reports about them", security = @SecurityRequirement(name = "jwtToken"))
    @PostMapping("/api/admin/users/{userId}/ban")
    public ResponseEntity<HttpResponse> ban(@PathVariable Long userId, @Valid @RequestBody BanRequest request,
                                            @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        moderationService.ban(userId, request.getReason(), lang);
        return HttpResponse.getResponseEntity(true, "User banned.");
    }

    @Operation(summary = "Admin: lift a ban", security = @SecurityRequirement(name = "jwtToken"))
    @DeleteMapping("/api/admin/users/{userId}/ban")
    public ResponseEntity<HttpResponse> unban(@PathVariable Long userId,
                                              @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        moderationService.unban(userId, lang);
        return HttpResponse.getResponseEntity(true, "Ban lifted.");
    }
}
