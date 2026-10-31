package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.request.UpdateUserProfileRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.UserProfileResponse;
import com.project.agriculturalblogapplication.service.AuthorProfileService;
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
import static com.project.agriculturalblogapplication.constatnt.AppConstants.LANG;

@Tag(name = "User profile - controller", description = "A user's public summary, keyed by user id.")
@RestController
@CommonApiResponses
@RequiredArgsConstructor
public class UserProfileController {

    private final AuthorProfileService authorProfileService;

    @Operation(summary = "A user's public summary: authorId, location, cover, counts and checked credentials (public)")
    @ApiResponse(content = @Content(schema = @Schema(implementation = UserProfileResponse.class)), responseCode = "200")
    @GetMapping("/api/users/{userId}/profile")
    public ResponseEntity<HttpResponse> get(@PathVariable Long userId,
                                            @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", authorProfileService.summary(userId, lang));
    }

    @Operation(summary = "Edit your own location and cover photo", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = UserProfileResponse.class)), responseCode = "200")
    @PutMapping("/api/user/profile")
    public ResponseEntity<HttpResponse> update(@Valid @RequestBody UpdateUserProfileRequest request,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Profile updated successfully.", authorProfileService.updateSummary(request, lang));
    }
}
