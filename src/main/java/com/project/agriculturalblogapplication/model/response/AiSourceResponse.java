package com.project.agriculturalblogapplication.model.response;

/**
 * One cited source of an AI answer: a post ({@code type} "blog", {@code blogId}) or a question whose vetted answers
 * were used ({@code type} "answer", {@code questionId}). {@code title} is the post's or the question's title.
 */
public record AiSourceResponse(String type, Long blogId, Long questionId, String title) {

    public static final String BLOG = "blog";

    public static final String ANSWER = "answer";

    /** Sources stored before answers were cited carry only a blogId. */
    public AiSourceResponse {
        if (type == null && blogId != null) {
            type = BLOG;
        }
    }

    public static AiSourceResponse blog(Long blogId, String title) {
        return new AiSourceResponse(BLOG, blogId, null, title);
    }

    public static AiSourceResponse answer(Long questionId, String title) {
        return new AiSourceResponse(ANSWER, null, questionId, title);
    }
}
