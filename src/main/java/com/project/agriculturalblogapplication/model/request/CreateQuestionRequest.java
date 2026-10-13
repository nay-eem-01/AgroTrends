package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.model.AgriInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateQuestionRequest {
    
    @NotBlank
    private String title;
    
    @NotBlank
    private String content;

    @NotNull(message = ErrorCode.ERROR_CATEGORY_IS_REQUIRED)
    private Long categoryId;

    /** Up to five tags, e.g. "rice blast"; new names are created. */
    @Size(max = AppConstants.MAX_TAGS_PER_POST, message = ErrorCode.ERROR_TOO_MANY_TAGS)
    private List<@NotBlank @Size(max = AppConstants.MAX_TAG_LENGTH, message = ErrorCode.ERROR_TAG_TOO_LONG) String> tags;

    /** Optional crop, season, region and soil the question is about. */
    @Valid
    private AgriInfo agri;
}
