package com.project.agriculturalblogapplication.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.ChangePasswordRequest;
import com.project.agriculturalblogapplication.model.request.UpdateUserRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.PublicUserResponse;
import com.project.agriculturalblogapplication.model.response.UserResponse;
import com.project.agriculturalblogapplication.service.UserService;
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
