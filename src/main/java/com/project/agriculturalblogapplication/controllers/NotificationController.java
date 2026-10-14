package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.NotificationResponse;
import com.project.agriculturalblogapplication.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Tag(name = "Notification - controller", description = "Your in-app notifications.")
@RestController
@RequestMapping("/api/notifications")
@CommonApiResponses
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Your notifications, newest first (unreadOnly=true for unread ones)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = NotificationResponse.class)), responseCode = "200")
    @GetMapping
    public ResponseEntity<HttpResponse> mine(@RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
                                             @RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                             @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                             @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.",
                notificationService.mine(unreadOnly, pageNo, pageSize, lang));
    }

    @Operation(summary = "How many of your notifications are unread: {count}", security = @SecurityRequirement(name = "jwtToken"))
    @GetMapping("/unread-count")
    public ResponseEntity<HttpResponse> unreadCount(@RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", Map.of("count", notificationService.unreadCount(lang)));
    }

    @Operation(summary = "Mark one of your notifications read", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = NotificationResponse.class)), responseCode = "200")
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<HttpResponse> markRead(@PathVariable Long notificationId,
                                                 @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Marked as read.", notificationService.markRead(notificationId, lang));
    }

    @Operation(summary = "Mark all your notifications read: {updated}", security = @SecurityRequirement(name = "jwtToken"))
    @PostMapping("/read-all")
    public ResponseEntity<HttpResponse> markAllRead(@RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "All marked as read.", Map.of("updated", notificationService.markAllRead(lang)));
    }
}
