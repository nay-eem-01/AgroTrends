package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Author;

/** What a reader sees of a post's author: never the e-mail, phone or roles of the user behind it. */
public record AuthorSummaryResponse(Long authorId, String name) {

    public static AuthorSummaryResponse from(Author author) {
        return new AuthorSummaryResponse(author.getId(), author.getUser().getName());
    }
}
