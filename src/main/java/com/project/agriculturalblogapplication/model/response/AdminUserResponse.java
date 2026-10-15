package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.util.CommonUtils;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/** A user as the admin dashboard sees them: contact details, roles and ban state. */
public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String mobileNumber,
        Set<String> roles,
        boolean emailVerified,
        boolean banned,
        Instant bannedAt,
        String banReason,
        Instant createdAt
) {

    public static AdminUserResponse from(User user) {
        Set<String> roles = user.getRoles() == null ? Set.of()
                : user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getMobileNumber(), roles,
                user.getEmailVerifiedAt() != null, user.getBannedAt() != null, CommonUtils.toInstant(user.getBannedAt()),
                user.getBanReason(), CommonUtils.toInstant(user.getCreationDate()));
    }
}
