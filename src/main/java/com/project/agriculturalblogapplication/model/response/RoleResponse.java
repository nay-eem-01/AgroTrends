package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.entities.Privilege;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.enums.RoleType;

import java.util.Comparator;
import java.util.List;

/** A role and its privileges; only {@code ADMIN} roles can be edited or granted. */
public record RoleResponse(Long id, String name, RoleType type, List<PrivilegeResponse> privileges) {

    public record PrivilegeResponse(Long id, String name, String description) {

        public static PrivilegeResponse from(Privilege privilege) {
            return new PrivilegeResponse(privilege.getId(), privilege.getPrivilegeName(), privilege.getDescription());
        }
    }

    public static RoleResponse from(Role role) {
        List<PrivilegeResponse> privileges = role.getPrivileges() == null ? List.of() : role.getPrivileges().stream()
                .map(PrivilegeResponse::from).sorted(Comparator.comparing(PrivilegeResponse::name)).toList();
        return new RoleResponse(role.getId(), role.getRoleName(), role.getRoleType(), privileges);
    }
}
