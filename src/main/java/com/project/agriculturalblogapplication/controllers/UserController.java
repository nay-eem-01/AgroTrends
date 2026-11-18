package com.project.agriculturalblogapplication.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.ChangePasswordRequest;
import com.project.agriculturalblogapplication.model.request.UpdateUserRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.PublicUserResponse;
import com.project.agriculturalblogapplication.model.response.UserResponse;
import com.project.agriculturalblogapplication.enums.MailKind;
import com.project.agriculturalblogapplication.model.request.UpdateUserSettingsRequest;
import com.project.agriculturalblogapplication.model.response.UserSettingsResponse;
import com.project.agriculturalblogapplication.service.UserService;
import com.project.agriculturalblogapplication.service.UserSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import java.net.URI;
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


import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Tag(name = "User - Consumer", description = "Consumer related operations.")
@CommonApiResponses
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    private final UserSettingsService userSettingsService;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    @Operation(summary = "Your e-mail language and e-mail switches", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = UserSettingsResponse.class)), responseCode = "200")
    @GetMapping("/settings")
    public ResponseEntity<HttpResponse> settings(@RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", userSettingsService.mine(lang));
    }

    @Operation(summary = "Change your settings; leave a field out to keep it", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = UserSettingsResponse.class)), responseCode = "200")
    @PutMapping("/settings")
    public ResponseEntity<HttpResponse> updateSettings(@Valid @RequestBody UpdateUserSettingsRequest request,
                                                       @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Settings saved.", userSettingsService.update(request, lang));
    }

    @Operation(summary = "The unsubscribe link in our e-mails: switches that kind off and opens the web's /unsubscribed page")
    @ApiResponse(responseCode = "302", description = "To {frontendUrl}/unsubscribed?kind=..., with &error=invalid for a bad link")
    @GetMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribeLink(@RequestParam(name = "token") String token,
                                                @RequestParam(name = "kind") MailKind kind) {
        String target = frontendUrl + "/unsubscribed?kind=" + kind.name()
                + (userSettingsService.unsubscribe(token, kind) ? "" : "&error=invalid");
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    @Operation(summary = "One-click unsubscribe (RFC 8058): what mail apps call from the List-Unsubscribe header")
    @PostMapping("/unsubscribe")
    public ResponseEntity<HttpResponse> unsubscribeOneClick(@RequestParam(name = "token") String token,
                                                            @RequestParam(name = "kind") MailKind kind) {
        if (!userSettingsService.unsubscribe(token, kind)) {
            return HttpResponse.getResponseEntity(HttpStatus.BAD_REQUEST, "This unsubscribe link is not valid.");
        }
        return HttpResponse.getResponseEntity(true, "Unsubscribed.");
    }

    @Operation(summary = "Get user info", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = UserResponse.class)), responseCode = "200")
    @GetMapping("/me")
    public ResponseEntity<HttpResponse> getUserInfo(@RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                userService.getMe(lang)
        );
    }

    @Operation(summary = "Another user's public profile (name only)", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = PublicUserResponse.class)), responseCode = "200")
    @GetMapping("/id/{id}")
    public ResponseEntity<HttpResponse> findById(@PathVariable Long id, @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) throws JsonProcessingException {
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                userService.getPublicProfile(id, lang));
    }

    @Operation(summary = "Delete User with User Details", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @DeleteMapping("/delete")
    public ResponseEntity<HttpResponse> deleteUser(@RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        User user = userService.getUserInfo(lang);
        userService.deleteUser(user);
        return HttpResponse.getResponseEntity(
                true,
                "User deleted successfully.");
    }

    @Operation(summary = "Update User with User Details", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @PutMapping("/update")
    public ResponseEntity<HttpResponse> update(@RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang,
                                               @Valid @RequestBody UpdateUserRequest request) {

        userService.update(request, lang);
        return HttpResponse.getResponseEntity(
                true,
                "User updated successfully.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<HttpResponse> changePassword(@RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang,
                                                       @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request, lang);
        return HttpResponse.getResponseEntity(
                true,
                "Password changed. Please sign in again.");
    }
}
