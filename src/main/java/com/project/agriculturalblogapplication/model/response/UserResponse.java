package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.util.CommonUtils;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A user's own account (or an admin's view of it): includes contact details, never the password hash.
 * {@code authorId} is the user's profile id ({@code /api/authors/{authorId}}), null until the profile is first needed.
 * {@code hasPassword} is false for a Google-only account (it sets a first password through "forgot password").
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String mobileNumber,
        Set<String> roles,
        boolean mustChangePassword,
        Instant createdAt,
        Long authorId,
        boolean hasPassword,
        boolean emailVerified
) {

    public static UserResponse from(User user, Long authorId) {
        Set<String> roles = user.getRoles() == null ? Set.of()
                : user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getMobileNumber(),
                roles,
                Boolean.TRUE.equals(user.getMustChangePassword()), CommonUtils.toInstant(user.getCreationDate()), authorId,
                user.getPassword() != null, user.getEmailVerifiedAt() != null);
    }
}
