package com.project.agriculturalblogapplication.model.request;

import jakarta.validation.Valid;
import com.project.agriculturalblogapplication.model.AgriInfo;
import java.util.List;
import jakarta.validation.constraints.Size;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateBlogRequest {

    @NotNull(message = ErrorCode.ERROR_CATEGORY_IS_REQUIRED)
    private Long categoryId;

    @NotBlank(message = ErrorCode.ERROR_TITLE_IS_REQUIRED)
    private String title;

    @NotBlank(message = ErrorCode.ERROR_CONTENT_IS_REQUIRED)
    private String content;

    private String imageUrl;
    /** DRAFT keeps the post private; omitted means PUBLISHED, as before drafts existed. */
    private BlogStatus status;
    /** Up to five topic tags, e.g. "rice blast"; new names are created. */
    @Size(max = AppConstants.MAX_TAGS_PER_BLOG, message = ErrorCode.ERROR_TOO_MANY_TAGS)
    private List<@NotBlank @Size(max = AppConstants.MAX_TAG_LENGTH, message = ErrorCode.ERROR_TAG_TOO_LONG) String> tags;
    /** Optional crop, season, region and soil the post is about. */
    @Valid
    private AgriInfo agri;
}
