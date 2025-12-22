package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateBlogRequest {

    @NotNull(message = ErrorCode.ERROR_AUTHOR_USER_ID_IS_REQUIRED)
    private Long authorUserId;

    @NotNull(message = ErrorCode.ERROR_CATEGORY_IS_REQUIRED)
    private Long categoryId;

    @NotBlank(message = ErrorCode.ERROR_TITLE_IS_REQUIRED)
    private String title;

    @NotBlank(message = ErrorCode.ERROR_CONTENT_IS_REQUIRED)
    private String content;

    private String imageUrl;
}
