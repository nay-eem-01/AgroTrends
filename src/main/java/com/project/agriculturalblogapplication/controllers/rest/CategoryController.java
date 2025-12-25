package com.project.agriculturalblogapplication.controllers.rest;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.model.request.CreateBlogRequest;
import com.project.agriculturalblogapplication.model.request.UpdateBlogRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.service.BlogService;
import com.project.agriculturalblogapplication.service.CategoryService;
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
import static com.project.agriculturalblogapplication.constatnt.AppConstants.ASC_OR_DESC;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.ASC_OR_DESC_VALUE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_PAGE_SIZE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_NO;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.PAGE_SIZE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.SORT_BY;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.SORT_BY_VALUE;

@Tag(name = "Category controller", description = "Category related operations.")
@RestController
@RequestMapping("/api/categories")
@CommonApiResponses
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Get all categories - paginated", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Category.class))), responseCode = "200")
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
                categoryService.getAll(paginationArgs));
    }

    @Operation(summary = "New category creation", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Category.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    @PostMapping(value = "/create")
    public ResponseEntity<HttpResponse> createNewCategory(@RequestParam String categoryName) {
        return HttpResponse.getResponseEntity(
                true, "Category created successfully.", categoryService.create(categoryName));
    }

    @Operation(summary = "Update category info", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = Category.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    @PutMapping(value = "/update/categoryId/{categoryId}")
    public ResponseEntity<HttpResponse> updateCategory(@RequestBody String categoryName, @PathVariable Long categoryId) {
        return HttpResponse.getResponseEntity(
                true, "Category updated successfully.", categoryService.update(categoryName, categoryId));
    }

    @Operation(summary = "Delete category", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @PreAuthorize("hasAuthority('BLOG_DELETE')")
    @DeleteMapping(value = "/id/{categoryId}/delete")
    public ResponseEntity<HttpResponse> deleteCategory(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return HttpResponse.getResponseEntity(true, "Category deleted successfully.");
    }
}
