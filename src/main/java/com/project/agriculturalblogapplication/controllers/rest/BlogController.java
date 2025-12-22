package com.project.agriculturalblogapplication.controllers.rest;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.model.request.CreateBlogRequest;
import com.project.agriculturalblogapplication.model.request.UpdateBlogRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Tag(name = "Blog controller", description = "Blog related operations.")
@RestController
@RequestMapping("/api/blogs")
@CommonApiResponses
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @Operation(summary = "Get all blogs - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Blog.class))), responseCode = "200")
    @GetMapping(value = "/all")
    public ResponseEntity<HttpResponse> getAll(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                               @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                               @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                               @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc
    ) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                blogService.getAll(paginationArgs));
    }

    @Operation(summary = "Get all blogs by category - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Blog.class))), responseCode = "200")
    @GetMapping(value = "/all/category/{categoryId}")
    public ResponseEntity<HttpResponse> getAllByCategory(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                         @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                         @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                                         @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc,
                                                         @PathVariable Long categoryId
    ) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                blogService.getAllByCategory(paginationArgs, categoryId));
    }

    @Operation(summary = "Get all blogs - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Blog.class))), responseCode = "200")
    @GetMapping(value = "/all/author/{authorId}")
    public ResponseEntity<HttpResponse> getAllByAuthor(@RequestParam(name = PAGE_NO, defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                                       @RequestParam(name = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                       @RequestParam(name = SORT_BY, defaultValue = SORT_BY_VALUE) String sortBy,
                                                       @RequestParam(name = ASC_OR_DESC, defaultValue = ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc,
                                                       @PathVariable Long authorId
    ) {
        PaginationArgs paginationArgs = new PaginationArgs(pageNo, pageSize, sortBy, ascOrDesc);
        return HttpResponse.getResponseEntity(
                true,
                "Data loaded successfully.",
                blogService.getAllByAuthor(paginationArgs, authorId));
    }

    @Operation(summary = "Get blog info by id", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Blog.class)), responseCode = "200")
    @GetMapping(value = "/id/{blogId}")
    public ResponseEntity<HttpResponse> findById(@PathVariable Long blogId) {
        return HttpResponse.getResponseEntity(
                true, "Data loaded successfully.", blogService.findByIdWithException(blogId));
    }

    @Operation(summary = "New blog creation", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Blog.class)), responseCode = "200")
//    @PreAuthorize("hasAuthority('BLOG_CREATE')")
    @PostMapping(value = "/create")
    public ResponseEntity<HttpResponse> createNewBlog(@Valid @RequestBody CreateBlogRequest request) {
        return HttpResponse.getResponseEntity(
                true, "Blog created successfully.", blogService.create(request));
    }

    @Operation(summary = "Update blog info", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Blog.class)), responseCode = "200")
//    @PreAuthorize("hasAuthority('BLOG_UPDATE')")
    @PutMapping(value = "/update")
    public ResponseEntity<HttpResponse> updateBlog(@Valid @RequestBody UpdateBlogRequest request) {
        return HttpResponse.getResponseEntity(
                true, "Blog updated successfully.", blogService.update(request));
    }

    @Operation(summary = "Delete blog", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
//    @PreAuthorize("hasAuthority('BLOG_DELETE')")
    @DeleteMapping(value = "/id/{blogId}/delete")
    public ResponseEntity<HttpResponse> deleteBlog(@PathVariable Long blogId) {
        blogService.delete(blogId);
        return HttpResponse.getResponseEntity(true, "Blog deleted successfully.");
    }
}
