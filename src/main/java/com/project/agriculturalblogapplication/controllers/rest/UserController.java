package com.project.agriculturalblogapplication.controllers.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Tag(name = "User - Consumer", description = "Consumer related operations.")
@CommonApiResponses
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get all users - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class))), responseCode = "200")
    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping(value = "/paginated")
    public ResponseEntity<HttpResponse> getAllPaginatedUsers(
            @RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
            @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
            @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
            @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc,
            @RequestParam(required = false) Map<String, Object> parameters
    ) {
        PaginationArgs paginationArgs = new PaginationArgs(
                pageNo, pageSize, sortBy, ascOrDesc);

        return HttpResponse.getResponseEntity(
                true,
                "All paginated users loaded",
                userService.getAllPaginatedUser(paginationArgs)
        );
    }

    @Operation(summary = "Get user info", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = User.class)), responseCode = "200")
    @GetMapping("/me")
    public ResponseEntity<HttpResponse> getUserInfo(@RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                userService.getUserInfo(lang)
        );
    }

    @Operation(summary = "Get user info by ID", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = User.class)), responseCode = "200")
    @GetMapping("/id/{id}")
    public ResponseEntity<HttpResponse> findById(@PathVariable Long id, @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) throws JsonProcessingException {
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                userService.findByIdWithException(id, lang));
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
}
