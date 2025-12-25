package com.project.agriculturalblogapplication.controllers.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.request.SignInRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.WebTokenResponse;
import com.project.agriculturalblogapplication.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;

@Tag(name = "Admin - controller", description = "Admin related operations.")
@RestController
@RequestMapping("/api/admin")
@CommonApiResponses
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @Operation(summary = "Sign in")
    @ApiResponse(content = @Content(schema = @Schema(implementation = WebTokenResponse.class)), responseCode = "200")
    @PostMapping(value = "/sign-in")
    public ResponseEntity<HttpResponse> signIn(@Valid @RequestBody SignInRequest request, @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) throws JsonProcessingException {
        return HttpResponse.getResponseEntity(
                true,
                "Sign-in successful.",
                authService.signInAsAdmin(request, lang));
    }
}
