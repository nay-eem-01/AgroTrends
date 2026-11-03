package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.service.SharePageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

/** Link previews: the URL people share on Facebook. Outside /api, plain HTML, public. */
@Tag(name = "Share", description = "Link-preview pages (Open Graph) for shared questions and stories.")
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

    private final SharePageService sharePageService;

    @Operation(summary = "Preview page for a question: Open Graph tags, then on to the web page")
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<String> question(@PathVariable Long questionId) {
        return html(sharePageService.question(questionId, shareUrl()));
    }

    @Operation(summary = "Preview page for a story: Open Graph tags, then on to the web page")
    @GetMapping("/stories/{slug}")
    public ResponseEntity<String> story(@PathVariable String slug) {
        return html(sharePageService.story(slug, shareUrl()));
    }

    private ResponseEntity<String> html(Optional<String> page) {
        // Facebook keeps its own copy anyway; an edit shows after a re-scrape.
        CacheControl cache = CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic();
        return page.map(body -> ResponseEntity.ok().cacheControl(cache).contentType(MediaType.TEXT_HTML).body(body))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(cache)
                        .contentType(MediaType.TEXT_HTML).body(sharePageService.notFound(shareUrl())));
    }

    /** og:url names this page itself: Facebook re-scrapes whatever og:url says. */
    private static String shareUrl() {
        return ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
    }
}
