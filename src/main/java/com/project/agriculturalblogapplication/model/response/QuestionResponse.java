package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.model.AgriInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuestionResponse {
    private Long questionId;
    private Long userId;
    private String title;
    private String content;

    /** The author's display name (never their e-mail). */
    private String authorName;

    private Instant createdAt;

    private Instant updatedAt;

    private AgriInfo agri;

    /** Null only on questions asked before topics existed. */
    private CategoryResponse category;

    private List<String> tags;

    private List<String> imageUrls;
}
