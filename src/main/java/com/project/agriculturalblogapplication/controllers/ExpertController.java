package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import com.project.agriculturalblogapplication.model.request.ExpertApplicationRequest;
import com.project.agriculturalblogapplication.model.request.ExpertDecisionRequest;
import com.project.agriculturalblogapplication.model.response.ExpertApplicationResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.ExpertService;
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

@Tag(name = "Expert - controller", description = "Verified Experts: applying, and the admin review of applications and badges.")
@RestController
@CommonApiResponses
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;

    @Operation(summary = "Apply to be a Verified Expert: topics, your field, proof images or links (one pending at a time)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ExpertApplicationResponse.class)), responseCode = "200")
    @PostMapping("/api/experts/applications")
    public ResponseEntity<HttpResponse> apply(@Valid @RequestBody ExpertApplicationRequest request,
                                              @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Application sent.", expertService.apply(request, lang));
    }

    @Operation(summary = "Your own expert applications and their decisions, newest first",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ExpertApplicationResponse.class)), responseCode = "200")
    @GetMapping("/api/experts/applications/me")
    public ResponseEntity<HttpResponse> mine(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                             @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                             @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", expertService.mine(pageNo, pageSize, lang));
    }

    @Operation(summary = "Admin: expert applications in one status (default PENDING), oldest first",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ExpertApplicationResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('EXPERT_REVIEW')")
    @GetMapping("/api/admin/expert-applications")
    public ResponseEntity<HttpResponse> applications(@RequestParam(name = "status", defaultValue = "PENDING") ApplicationStatus status,
                                                     @RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                     @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.",
                expertService.applications(status, pageNo, pageSize));
    }

    @Operation(summary = "Admin: approve a pending application; grants a Verified Expert badge in each topic "
            + "(all requested, or the subset in categoryIds)", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ExpertApplicationResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('EXPERT_REVIEW')")
    @PostMapping("/api/admin/expert-applications/{applicationId}/approve")
    public ResponseEntity<HttpResponse> approve(@PathVariable Long applicationId, @Valid @RequestBody ExpertDecisionRequest decision,
                                                @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Application approved.", expertService.approve(applicationId, decision, lang));
    }

    @Operation(summary = "Admin: reject a pending application with a reason the applicant sees",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ExpertApplicationResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('EXPERT_REVIEW')")
    @PostMapping("/api/admin/expert-applications/{applicationId}/reject")
    public ResponseEntity<HttpResponse> reject(@PathVariable Long applicationId, @Valid @RequestBody ExpertDecisionRequest decision,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Application rejected.", expertService.reject(applicationId, decision, lang));
    }

    @Operation(summary = "Admin: revoke a user's Verified Expert badge in one topic", security = @SecurityRequirement(name = "jwtToken"))
    @PreAuthorize("hasAuthority('EXPERT_REVIEW')")
    @DeleteMapping("/api/admin/users/{userId}/expert-badges/{categoryId}")
    public ResponseEntity<HttpResponse> revoke(@PathVariable Long userId, @PathVariable Long categoryId,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        expertService.revoke(userId, categoryId, lang);
        return HttpResponse.getResponseEntity(true, "Badge revoked.");
    }
}
