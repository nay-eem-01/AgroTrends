package com.project.agriculturalblogapplication.util;

import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.security.service.CustomUserDetails;
import com.project.agriculturalblogapplication.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtil {

    private AuthUtil() {}

    /** The authenticated caller, or null for anonymous requests. Only id, e-mail and roles are populated. */
    public static User getLoggedInUser(UserService userService) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            User user = new User();
            user.setId(customUserDetails.getId());
            user.setEmail(customUserDetails.getEmail());
            user.setRoles(customUserDetails.getRoles());
            return user;
        }

        return userService.findByEmail(authentication.getName());
    }
}
