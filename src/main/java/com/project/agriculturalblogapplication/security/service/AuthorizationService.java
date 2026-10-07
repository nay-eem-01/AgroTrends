package com.project.agriculturalblogapplication.security.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.entities.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single place that answers "who is calling?" and "may they touch this?".
 * Identity always comes from the validated JWT, never from a request body.
 */
@Component
public class AuthorizationService {

    public CustomUserDetails currentPrincipal(String lang) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_UNAUTHORIZED_ACCESS, lang);
        }
        return principal;
    }

    public Long currentUserId(String lang) {
        return currentPrincipal(lang).getId();
    }

    public boolean isAdmin(CustomUserDetails principal) {
        if (principal.getRoles() == null) {
            return false;
        }
        for (Role role : principal.getRoles()) {
            if (role.getRoleType() == RoleType.ADMIN || role.getRoleType() == RoleType.SUPER_ADMIN) {
                return true;
            }
        }
        return false;
    }

    public boolean isOwnerOrAdmin(Long ownerUserId, String lang) {
        CustomUserDetails principal = currentPrincipal(lang);
        return isAdmin(principal) || (ownerUserId != null && ownerUserId.equals(principal.getId()));
    }

    /** Passes for the owner of a resource or any admin; otherwise 403. */
    public void assertOwnerOrAdmin(Long ownerUserId, String lang) {
        if (!isOwnerOrAdmin(ownerUserId, lang)) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_FORBIDDEN, lang);
        }
    }
}
