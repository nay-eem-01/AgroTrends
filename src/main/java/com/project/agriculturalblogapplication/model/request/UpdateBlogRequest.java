package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class UpdateBlogRequest {

    @NotNull(message = ErrorCode.ERROR_BLOG_ID_IS_REQUIRED)
    private Long blogId;

    @NotBlank(message = ErrorCode.ERROR_TITLE_IS_REQUIRED)
    private String title;

    @NotBlank(message = ErrorCode.ERROR_CONTENT_IS_REQUIRED)
    private String content;

    private String imageUrl;

    @NotNull(message = ErrorCode.ERROR_CATEGORY_IS_REQUIRED)
    private Long categoryId;
}
