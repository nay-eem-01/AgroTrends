package com.project.agriculturalblogapplication.Controllers.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.agriculturalblogapplication.Config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.*;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.WebTokenResponse;
import com.project.agriculturalblogapplication.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;

@Tag(name = "Auth - User", description = "Authentication related operations.")
@RestController
@RequestMapping("/api/auth")
@CommonApiResponses
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Sign up")
    @ApiResponse(content = @Content(schema = @Schema(implementation = User.class)), responseCode = "200")
    @PostMapping(value = "/sign-up")
    public ResponseEntity<HttpResponse> signUp(@Valid @RequestBody SignUpRequest request, @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(
                HttpStatus.OK,
                "Sign-up successful.",
                authService.signUp(request, lang));
    }

    @Operation(summary = "Sign in")
    @ApiResponse(content = @Content(schema = @Schema(implementation = WebTokenResponse.class)), responseCode = "200")
    @PostMapping(value = "/sign-in")
    public ResponseEntity<HttpResponse> signIn(@Valid @RequestBody SignInRequest request, @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) throws JsonProcessingException {
        return HttpResponse.getResponseEntity(
                true,
                "Sign-in successful.",
                authService.signIn(request, lang));
    }

    @Operation(summary = "Sign-out.", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @GetMapping(value = "/sign-out")
    public ResponseEntity<HttpResponse> signOut(HttpServletRequest httpServletRequest, @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        authService.signOut(httpServletRequest, lang);
        return HttpResponse.getResponseEntity(
                true,
                "You are signed out.");
    }
}
