package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.model.request.CreateBlogRequest;
import com.project.agriculturalblogapplication.model.request.UpdateBlogRequest;
import com.project.agriculturalblogapplication.model.response.BlogResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.RelatedBlogResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.service.BlogService;
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

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Tag(name = "Blog controller", description = "Blog related operations.")
@RestController
@RequestMapping("/api/blogs")
@CommonApiResponses
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @Operation(summary = "Get all blogs - paginated")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = BlogResponse.class))), responseCode = "200")
    @GetMapping(value = "/all")
    public ResponseEntity<HttpResponse> getAll(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                               @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                               @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                               @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc,
                                               @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang
    ) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                blogService.getAll(paginationArgs, lang));
    }

    @Operation(summary = "Get all blogs by category - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = BlogResponse.class))), responseCode = "200")
    @GetMapping(value = "/all/category/{categoryId}")
    public ResponseEntity<HttpResponse> getAllByCategory(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                         @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                         @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                                         @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc,
                                                         @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang,
                                                         @PathVariable Long categoryId
    ) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                blogService.getAllByCategory(paginationArgs, categoryId, lang));
    }

    @Operation(summary = "Get all blogs by an author (the author.authorId shown on a blog) - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = BlogResponse.class))), responseCode = "200")
    @GetMapping(value = "/all/author/{authorId}")
    public ResponseEntity<HttpResponse> getAllByAuthor(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                       @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                       @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                                       @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc,
                                                       @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang,
                                                       @PathVariable Long authorId
    ) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                blogService.getAllByAuthor(paginationArgs, authorId, lang));
    }

    @Operation(summary = "Get blog info by id", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = BlogResponse.class)), responseCode = "200")
    @GetMapping(value = "/id/{blogId}")
    public ResponseEntity<HttpResponse> findById(@PathVariable Long blogId) {
        return HttpResponse.getResponseEntity(
                true, "Data loaded successfully.", blogService.getById(blogId));
    }

    @Operation(summary = "Other posts on similar topics, most similar first (found by meaning, not keywords)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = RelatedBlogResponse.class)), responseCode = "200")
    @GetMapping(value = "/id/{blogId}/related")
    public ResponseEntity<HttpResponse> related(@PathVariable Long blogId,
                                                @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return HttpResponse.getResponseEntity(
                true, "Data loaded successfully.", blogService.related(blogId, limit));
    }

    @Operation(summary = "New blog creation", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = BlogResponse.class)), responseCode = "200")
    @PostMapping(value = "/create")
    public ResponseEntity<HttpResponse> createNewBlog(@Valid @RequestBody CreateBlogRequest request,
                                                      @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(
                true, "Blog created successfully.", blogService.create(request, lang));
    }

    @Operation(summary = "Update blog info", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = BlogResponse.class)), responseCode = "200")
    @PutMapping(value = "/update")
    public ResponseEntity<HttpResponse> updateBlog(@Valid @RequestBody UpdateBlogRequest request,
                                                   @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(
                true, "Blog updated successfully.", blogService.update(request, lang));
    }

    @Operation(summary = "Delete blog", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @DeleteMapping(value = "/id/{blogId}/delete")
    public ResponseEntity<HttpResponse> deleteBlog(@PathVariable Long blogId,
                                                   @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        blogService.delete(blogId, lang);
        return HttpResponse.getResponseEntity(true, "Blog deleted successfully.");
    }
}
