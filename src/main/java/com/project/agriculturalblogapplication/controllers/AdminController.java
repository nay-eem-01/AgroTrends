package com.project.agriculturalblogapplication.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.request.SignInRequest;
import com.project.agriculturalblogapplication.model.response.AdminUserDetailResponse;
import com.project.agriculturalblogapplication.model.response.AdminUserResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.WebTokenResponse;
import com.project.agriculturalblogapplication.service.AdminService;
import com.project.agriculturalblogapplication.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_SIZE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.LANG;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_SIZE;

@Tag(name = "Admin - controller", description = "Admin related operations.")
@RestController
@RequestMapping("/api/admin")
@CommonApiResponses
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    private final AdminService adminService;

    @Operation(summary = "Sign in")
    @ApiResponse(content = @Content(schema = @Schema(implementation = WebTokenResponse.class)), responseCode = "200")
    @PostMapping(value = "/sign-in")
    public ResponseEntity<HttpResponse> signIn(@Valid @RequestBody SignInRequest request,
                                               @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang,
                                               HttpServletRequest httpServletRequest) throws JsonProcessingException {
        return HttpResponse.getResponseEntity(
                true,
                "Sign-in successful.",
                authService.signInAsAdmin(request, lang, httpServletRequest.getRemoteAddr()));
    }

    @Operation(summary = "Admin: users whose name or e-mail contains q; banned=true/false filters by ban state; newest first",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AdminUserResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping("/users")
    public ResponseEntity<HttpResponse> users(@RequestParam(name = "q", required = false) String query,
                                              @RequestParam(name = "banned", required = false) Boolean banned,
                                              @RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                              @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", adminService.users(query, banned, pageNo, pageSize));
    }

    @Operation(summary = "Admin: one user with their post counts, open reports about the account and expert topics",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AdminUserDetailResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping("/users/{userId}")
    public ResponseEntity<HttpResponse> user(@PathVariable Long userId,
                                             @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", adminService.user(userId, lang));
    }
}
