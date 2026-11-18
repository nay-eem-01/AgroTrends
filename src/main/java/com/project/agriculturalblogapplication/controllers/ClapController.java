package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.response.ClapResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.ClapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.LANG;

@Tag(name = "Clap - controller", description = "Claps on published blogs.")
@RestController
@RequestMapping("/api/blogs/id/{blogId}/claps")
@CommonApiResponses
@RequiredArgsConstructor
public class ClapController {

    private final ClapService clapService;

    @Operation(summary = "A blog's total claps and yours", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ClapResponse.class)), responseCode = "200")
    @GetMapping
    public ResponseEntity<HttpResponse> get(@PathVariable Long blogId,
                                            @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", clapService.get(blogId, lang));
    }

    @Operation(summary = "Clap for a blog (1-50 at a time; at most 50 per reader per blog; not your own)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ClapResponse.class)), responseCode = "200")
    @PostMapping
    public ResponseEntity<HttpResponse> clap(@PathVariable Long blogId,
                                             @RequestParam(name = "count", defaultValue = "1") int count,
                                             @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Clapped.", clapService.clap(blogId, count, lang));
    }

    @Operation(summary = "Take back all your claps on a blog", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = ClapResponse.class)), responseCode = "200")
    @DeleteMapping
    public ResponseEntity<HttpResponse> removeMyClaps(@PathVariable Long blogId,
                                                      @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Claps removed.", clapService.removeMyClaps(blogId, lang));
    }
}
