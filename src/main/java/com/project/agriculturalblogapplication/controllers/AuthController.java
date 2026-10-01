package com.project.agriculturalblogapplication.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.*;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.WebTokenResponse;
import com.project.agriculturalblogapplication.service.AuthService;
import com.project.agriculturalblogapplication.service.PasswordResetService;
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

    private final PasswordResetService passwordResetService;

    @Operation(summary = "Sign up")
    @ApiResponse(content = @Content(schema = @Schema(implementation = User.class)), responseCode = "200")
    @PostMapping(value = "/sign-up")
    public ResponseEntity<HttpResponse> signUp(@Valid @RequestBody SignUpRequest request,
                                               @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(
                HttpStatus.OK,
                "Sign-up successful.",
                authService.signUp(request, lang));
    }

    @Operation(summary = "Sign in")
    @ApiResponse(content = @Content(schema = @Schema(implementation = WebTokenResponse.class)), responseCode = "200")
    @PostMapping(value = "/sign-in")
    public ResponseEntity<HttpResponse> signIn(@Valid @RequestBody SignInRequest request,
                                               @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang,
                                               HttpServletRequest httpServletRequest) throws JsonProcessingException {
        return HttpResponse.getResponseEntity(
                true,
                "Sign-in successful.",
                authService.signIn(request, lang, httpServletRequest.getRemoteAddr()));
    }

    @Operation(summary = "Exchange a refresh token for a new access token (the refresh token is single-use and rotated)")
    @ApiResponse(content = @Content(schema = @Schema(implementation = WebTokenResponse.class)), responseCode = "200")
    @PostMapping(value = "/refresh-token")
    public ResponseEntity<HttpResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                                     @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(
                true,
                "Token refreshed.",
                authService.refreshToken(request, lang));
    }

    @Operation(summary = "Request a password-reset e-mail. Always answers 200 so accounts cannot be enumerated.")
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @PostMapping(value = "/forgot-password")
    public ResponseEntity<HttpResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                       @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang,
                                                       HttpServletRequest httpServletRequest) {
        passwordResetService.requestReset(request.getEmail(), httpServletRequest.getRemoteAddr(), lang);
        return HttpResponse.getResponseEntity(
                true,
                "If an account exists for that e-mail, a reset link has been sent.");
    }

    @Operation(summary = "Set a new password using the token from the reset e-mail")
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @PostMapping(value = "/reset-password")
    public ResponseEntity<HttpResponse> resetPassword(@Valid @RequestBody ResetPasswordWithTokenRequest request,
                                                      @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        passwordResetService.reset(request.getToken(), request.getNewPassword(), lang);
        return HttpResponse.getResponseEntity(
                true,
                "Password has been reset. Please sign in with your new password.");
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
