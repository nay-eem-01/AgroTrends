package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.response.BlogResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Tag(name = "Bookmark - controller", description = "Your private reading list.")
@RestController
@RequestMapping("/api")
@CommonApiResponses
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "Your saved blogs, most recently saved first", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = BlogResponse.class))), responseCode = "200")
    @GetMapping("/bookmarks")
    public ResponseEntity<HttpResponse> mine(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                             @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                             @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", bookmarkService.mine(pageNo, pageSize, lang));
    }

    @Operation(summary = "Save a blog to your reading list (repeating it is harmless)", security = @SecurityRequirement(name = "jwtToken"))
    @PutMapping("/blogs/id/{blogId}/bookmark")
    public ResponseEntity<HttpResponse> add(@PathVariable Long blogId,
                                            @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        bookmarkService.add(blogId, lang);
        return HttpResponse.getResponseEntity(true, "Saved to your reading list.");
    }

    @Operation(summary = "Remove a blog from your reading list (repeating it is harmless)", security = @SecurityRequirement(name = "jwtToken"))
    @DeleteMapping("/blogs/id/{blogId}/bookmark")
    public ResponseEntity<HttpResponse> remove(@PathVariable Long blogId,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        bookmarkService.remove(blogId, lang);
        return HttpResponse.getResponseEntity(true, "Removed from your reading list.");
    }
}
