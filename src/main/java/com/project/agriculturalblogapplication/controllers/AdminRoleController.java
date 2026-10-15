package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.request.CreateRoleRequest;
import com.project.agriculturalblogapplication.model.request.UpdateRoleRequest;
import com.project.agriculturalblogapplication.model.response.AdminUserResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.RoleResponse;
import com.project.agriculturalblogapplication.model.response.RoleResponse.PrivilegeResponse;
import com.project.agriculturalblogapplication.service.PrivilegeService;
import com.project.agriculturalblogapplication.service.RoleService;
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

import java.util.Comparator;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.LANG;

/**
 * Who may do what in the admin dashboard (roadmap 9.3). ROLE_* privileges are never grantable, so only the
 * super-admin reaches these endpoints.
 */
@Tag(name = "Admin - roles", description = "Admin roles and who holds them.")
@RestController
@RequestMapping("/api/admin")
@CommonApiResponses
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;

    private final PrivilegeService privilegeService;

    @Operation(summary = "Super-admin: privileges an admin role may hold", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = PrivilegeResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @GetMapping("/privileges")
    public ResponseEntity<HttpResponse> privileges() {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", privilegeService.getAllPrivileges().stream()
                .filter(RoleService::isGrantable).map(PrivilegeResponse::from)
                .sorted(Comparator.comparing(PrivilegeResponse::name)).toList());
    }

    @Operation(summary = "Super-admin: every role with its privileges", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = RoleResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @GetMapping("/roles")
    public ResponseEntity<HttpResponse> roles() {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.",
                roleService.getAllRoles().stream().map(RoleResponse::from).toList());
    }

    @Operation(summary = "Super-admin: create an ADMIN role", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = RoleResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    @PostMapping("/roles")
    public ResponseEntity<HttpResponse> create(@Valid @RequestBody CreateRoleRequest request,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Role created.", RoleResponse.from(roleService.createRole(request, lang)));
    }

    @Operation(summary = "Super-admin: rename an ADMIN role or change its privileges", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = RoleResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    @PutMapping("/roles")
    public ResponseEntity<HttpResponse> update(@Valid @RequestBody UpdateRoleRequest request,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Role updated.", RoleResponse.from(roleService.updateRole(request, lang)));
    }

    @Operation(summary = "Super-admin: delete an ADMIN role nobody holds", security = @SecurityRequirement(name = "jwtToken"))
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<HttpResponse> delete(@PathVariable Long roleId,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        roleService.deleteRole(roleId, lang);
        return HttpResponse.getResponseEntity(true, "Role deleted.");
    }

    @Operation(summary = "Super-admin: give a user an ADMIN role (takes effect on their next request)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AdminUserResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    @PostMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<HttpResponse> grant(@PathVariable Long userId, @PathVariable Long roleId,
                                              @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Role granted.",
                AdminUserResponse.from(roleService.grantRole(userId, roleId, lang)));
    }

    @Operation(summary = "Super-admin: take an ADMIN role away from a user", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = AdminUserResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<HttpResponse> remove(@PathVariable Long userId, @PathVariable Long roleId,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Role removed.",
                AdminUserResponse.from(roleService.removeRole(userId, roleId, lang)));
    }
}
