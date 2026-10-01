package com.project.agriculturalblogapplication.security;

import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationServiceTest {

    private final AuthorizationService authorization = new AuthorizationService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void signInAs(long id, RoleType type) {
        Role role = new Role();
        role.setRoleType(type);
        CustomUserDetails details = new CustomUserDetails(id, "n", "u" + id + "@x.com", "pw", Set.of(role), List.of(), false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    void ownerMayTouchOwnResource() {
        signInAs(7, RoleType.USER);
        assertDoesNotThrow(() -> authorization.assertOwnerOrAdmin(7L, "en"));
    }

    @Test
    void otherUserIsForbidden() {
        signInAs(7, RoleType.USER);
        ApplicationException ex = assertThrows(ApplicationException.class, () -> authorization.assertOwnerOrAdmin(8L, "en"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    void adminMayTouchAnyResource() {
        signInAs(1, RoleType.SUPER_ADMIN);
        assertDoesNotThrow(() -> authorization.assertOwnerOrAdmin(99L, "en"));
    }

    @Test
    void anonymousCallerIsUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> authorization.currentUserId("en"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }
}
