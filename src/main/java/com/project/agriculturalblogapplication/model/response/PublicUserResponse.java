package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.User;

/** What any signed-in user may see about another user. */
public record PublicUserResponse(Long id, String name) {

    public static PublicUserResponse from(User user) {
        return new PublicUserResponse(user.getId(), user.getName());
    }
}
