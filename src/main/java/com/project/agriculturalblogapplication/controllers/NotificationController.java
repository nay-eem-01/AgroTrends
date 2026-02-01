package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.model.request.CreateBlogRequest;
import com.project.agriculturalblogapplication.model.request.UpdateBlogRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.NotificationResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.ASC_OR_DESC;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.ASC_OR_DESC_VALUE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_SIZE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_SIZE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.SORT_BY;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.SORT_BY_VALUE;

@Tag(name = "Notification", description = "Notification related operations.")
@RestController
@RequestMapping("/api/notifications")
@CommonApiResponses
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get all notification by user id - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResponse.class))), responseCode = "200")
    @GetMapping(value = "/all/{userId}")
    public ResponseEntity<HttpResponse> getAllByUserId(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                       @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                       @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                                       @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc,
                                                       @PathVariable Long userId
    ) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                notificationService.getAllByUserId(paginationArgs, userId));
    }

    @Operation(summary = "Get all unread notification by user id - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResponse.class))), responseCode = "200")
    @GetMapping(value = "/all/un-read/{userId}")
    public ResponseEntity<HttpResponse> getAllUnreadNotifications(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                                  @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                                  @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                                                  @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc,
                                                                  @PathVariable Long userId
    ) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                notificationService.getAllUnreadNotifications(paginationArgs, userId));
    }

    @Operation(summary = "Get unread count", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = NotificationResponse.class)), responseCode = "200")
    @GetMapping(value = "/un-read/count/{userId}")
    public ResponseEntity<HttpResponse> getUnreadCount(@PathVariable Long userId) {
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                notificationService.getUnreadCount(userId));
    }

    @Operation(summary = "Mark as read", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = NotificationResponse.class)), responseCode = "200")
    @PutMapping(value = "/{id}/read")
    public ResponseEntity<HttpResponse> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return HttpResponse.getResponseEntity(true, "Notification marked as read.");
    }

    @Operation(summary = "Mark all as read", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = NotificationResponse.class)), responseCode = "200")
    @PutMapping(value = "/{userId}/read-all")
    public ResponseEntity<HttpResponse> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return HttpResponse.getResponseEntity(true, "All notification marked as read.");
    }
}
