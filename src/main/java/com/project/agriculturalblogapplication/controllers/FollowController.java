package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.response.AuthorSummaryResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.FollowService;
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

@Tag(name = "Follow - controller", description = "Following authors and topics.")
@RestController
@RequestMapping("/api")
@CommonApiResponses
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "Follow an author (by author.authorId; repeating it is harmless)", security = @SecurityRequirement(name = "jwtToken"))
    @PutMapping("/authors/{authorId}/follow")
    public ResponseEntity<HttpResponse> followAuthor(@PathVariable Long authorId,
                                                     @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        followService.followAuthor(authorId, lang);
        return HttpResponse.getResponseEntity(true, "Following.");
    }

    @Operation(summary = "Stop following an author", security = @SecurityRequirement(name = "jwtToken"))
    @DeleteMapping("/authors/{authorId}/follow")
    public ResponseEntity<HttpResponse> unfollowAuthor(@PathVariable Long authorId,
                                                       @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        followService.unfollowAuthor(authorId, lang);
        return HttpResponse.getResponseEntity(true, "Unfollowed.");
    }

    @Operation(summary = "Follow a topic tag (repeating it is harmless)", security = @SecurityRequirement(name = "jwtToken"))
    @PutMapping("/tags/{tagName}/follow")
    public ResponseEntity<HttpResponse> followTag(@PathVariable String tagName,
                                                  @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        followService.followTag(tagName, lang);
        return HttpResponse.getResponseEntity(true, "Following.");
    }

    @Operation(summary = "Stop following a topic tag", security = @SecurityRequirement(name = "jwtToken"))
    @DeleteMapping("/tags/{tagName}/follow")
    public ResponseEntity<HttpResponse> unfollowTag(@PathVariable String tagName,
                                                    @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        followService.unfollowTag(tagName, lang);
        return HttpResponse.getResponseEntity(true, "Unfollowed.");
    }

    @Operation(summary = "Authors you follow, most recently followed first", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuthorSummaryResponse.class))), responseCode = "200")
    @GetMapping("/me/following/authors")
    public ResponseEntity<HttpResponse> followedAuthors(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                        @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                        @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", followService.followedAuthors(pageNo, pageSize, lang));
    }

    @Operation(summary = "Topic tags you follow, most recently followed first", security = @SecurityRequirement(name = "jwtToken"))
    @GetMapping("/me/following/tags")
    public ResponseEntity<HttpResponse> followedTags(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                     @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                     @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", followService.followedTags(pageNo, pageSize, lang));
    }
}
