package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Privilege;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateRoleRequest;
import com.project.agriculturalblogapplication.model.request.UpdateRoleRequest;
import com.project.agriculturalblogapplication.repositories.RoleRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceTest {

    private static final String LANG = "en";

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PrivilegeService privilegeService = mock(PrivilegeService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleService roles = new RoleService(roleRepository, privilegeService, userRepository);

    RoleServiceTest() {
        when(roleRepository.save(any(Role.class))).thenAnswer(call -> call.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(privilegeService.findByIdWithException(1L)).thenReturn(privilege(1L, "CONTENT_MODERATE"));
        when(privilegeService.findByIdWithException(2L)).thenReturn(privilege(2L, "ROLE_UPDATE"));
    }

    @Test
    void anAdminRoleIsCreatedWithItsPrivileges() {
        Role role = roles.createRole(new CreateRoleRequest("Moderator", RoleType.ADMIN, null, List.of(1L, 1L)), LANG);

        assertEquals(RoleType.ADMIN, role.getRoleType());
        assertEquals(1, role.getPrivileges().size());
    }

    @Test
    void onlyAdminRolesCanBeCreated() {
        for (RoleType type : List.of(RoleType.USER, RoleType.SUPER_ADMIN)) {
            assertRefused(ErrorCode.ERROR_ONLY_ADMIN_ROLES,
                    () -> roles.createRole(new CreateRoleRequest("x", type, null, List.of(1L)), LANG));
        }
    }

    @Test
    void roleManagementPrivilegesCannotBeHandedOut() {
        // Otherwise an admin role holder could make more admins, or a role with every privilege.
        assertRefused(ErrorCode.ERROR_PRIVILEGE_NOT_GRANTABLE,
                () -> roles.createRole(new CreateRoleRequest("x", RoleType.ADMIN, null, List.of(1L, 2L)), LANG));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void theSeededRolesCannotBeEditedOrDeleted() {
        when(roleRepository.findById(9L)).thenReturn(Optional.of(role(9L, RoleType.SUPER_ADMIN)));

        assertRefused(ErrorCode.ERROR_ONLY_ADMIN_ROLES,
                () -> roles.updateRole(new UpdateRoleRequest(9L, "x", RoleType.ADMIN, null, List.of(1L)), LANG));
        assertRefused(ErrorCode.ERROR_ONLY_ADMIN_ROLES, () -> roles.deleteRole(9L, LANG));
        verify(roleRepository, never()).delete(any());
    }

    @Test
    void anAdminRoleCannotBeTurnedIntoAnotherType() {
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role(5L, RoleType.ADMIN)));

        assertRefused(ErrorCode.ERROR_ONLY_ADMIN_ROLES,
                () -> roles.updateRole(new UpdateRoleRequest(5L, "x", RoleType.SUPER_ADMIN, null, List.of(1L)), LANG));
    }

    @Test
    void grantingAndRemovingAnAdminRoleChangesTheUsersRoles() {
        Role moderator = role(5L, RoleType.ADMIN);
        when(roleRepository.findById(5L)).thenReturn(Optional.of(moderator));
        User user = new User();
        user.setRoles(new HashSet<>(Set.of(role(1L, RoleType.USER))));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertTrue(roles.grantRole(7L, 5L, LANG).getRoles().contains(moderator));
        assertEquals(1, roles.removeRole(7L, 5L, LANG).getRoles().size());
    }

    @Test
    void theUserAndSuperAdminRolesCannotBeGrantedOrRemoved() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, RoleType.USER)));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role(2L, RoleType.SUPER_ADMIN)));

        assertRefused(ErrorCode.ERROR_ONLY_ADMIN_ROLES, () -> roles.grantRole(7L, 2L, LANG));
        assertRefused(ErrorCode.ERROR_ONLY_ADMIN_ROLES, () -> roles.removeRole(7L, 1L, LANG));
        verify(userRepository, never()).save(any());
    }

    private static void assertRefused(String message, Runnable call) {
        ApplicationException error = assertThrows(ApplicationException.class, call::run);
        assertEquals(message, error.getMessage());
    }

    private static Role role(Long id, RoleType type) {
        Role role = new Role();
        role.setId(id);
        role.setRoleName(type.name());
        role.setRoleType(type);
        role.setPrivileges(new ArrayList<>());
        return role;
    }

    private static Privilege privilege(Long id, String name) {
        Privilege privilege = new Privilege();
        privilege.setId(id);
        privilege.setPrivilegeName(name);
        return privilege;
    }
}
