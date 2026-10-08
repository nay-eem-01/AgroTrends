package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.request.UpdateAuthorProfileRequest;
import com.project.agriculturalblogapplication.model.response.AuthorProfileResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
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

@Tag(name = "Author - controller", description = "Public author profiles.")
@RestController
@RequestMapping("/api/authors")
@CommonApiResponses
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorProfileService authorProfileService;

    @Operation(summary = "An author's public profile with post and follower counts (posts: /api/blogs/all/author/{authorId}); public, followedByMe is false when not signed in")
    @ApiResponse(content = @Content(schema = @Schema(implementation = AuthorProfileResponse.class)), responseCode = "200")
    @GetMapping("/{authorId}")
    public ResponseEntity<HttpResponse> get(@PathVariable Long authorId,
                                            @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", authorProfileService.get(authorId, lang));
    }

    @Operation(summary = "Your own author profile, for the profile editor (authors only; 403 for readers)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AuthorProfileResponse.class)), responseCode = "200")
    @GetMapping("/me")
    public ResponseEntity<HttpResponse> getMine(@RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", authorProfileService.getMine(lang));
    }

    @Operation(summary = "Edit your own author profile (authors only)", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AuthorProfileResponse.class)), responseCode = "200")
    @PutMapping("/me")
    public ResponseEntity<HttpResponse> updateMine(@Valid @RequestBody UpdateAuthorProfileRequest request,
                                                   @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Profile updated successfully.", authorProfileService.updateMine(request, lang));
    }
}
