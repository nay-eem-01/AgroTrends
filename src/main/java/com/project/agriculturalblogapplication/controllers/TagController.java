package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tag - controller", description = "Topic tags on blogs.")
@RestController
@RequestMapping("/api/tags")
@CommonApiResponses
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "Up to 20 tag names starting with q (empty q lists the first 20), for autocomplete")
    @GetMapping
    public ResponseEntity<HttpResponse> suggest(@RequestParam(name = "q", defaultValue = "") String q) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", tagService.suggest(q));
    }
}
