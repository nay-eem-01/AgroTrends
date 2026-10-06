package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.util.CommonUtils;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/** A user's own account (or an admin's view of it): includes contact details, never the password hash. */
public record UserResponse(
        Long id,
        String name,
        String email,
        String mobileNumber,
        Set<String> userTypes,
        Set<String> roles,
        boolean mustChangePassword,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        Set<String> roles = user.getRoles() == null ? Set.of()
                : user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getMobileNumber(),
                user.getUserTypes() == null ? Set.of() : Set.copyOf(user.getUserTypes()), roles,
                Boolean.TRUE.equals(user.getMustChangePassword()), CommonUtils.toInstant(user.getCreationDate()));
    }
}
