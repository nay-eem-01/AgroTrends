package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.response.BlogResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Tag(name = "Feed - controller", description = "Home page feeds.")
@RestController
@RequestMapping("/api/feed")
@CommonApiResponses
@RequiredArgsConstructor
public class FeedController {

    private final BlogService blogService;

    @Operation(summary = "Newest published posts")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = BlogResponse.class))), responseCode = "200")
    @GetMapping("/latest")
    public ResponseEntity<HttpResponse> latest(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                               @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", blogService.latestFeed(pageNo, pageSize));
    }

    @Operation(summary = "Newest posts from authors and topics you follow (empty if you follow nothing)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = BlogResponse.class))), responseCode = "200")
    @GetMapping("/following")
    public ResponseEntity<HttpResponse> following(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                  @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                  @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", blogService.followingFeed(pageNo, pageSize, lang));
    }

    @Operation(summary = "Posts from the last 14 days, most clapped first")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = BlogResponse.class))), responseCode = "200")
    @GetMapping("/trending")
    public ResponseEntity<HttpResponse> trending(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                 @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", blogService.trendingFeed(pageNo, pageSize));
    }
}
