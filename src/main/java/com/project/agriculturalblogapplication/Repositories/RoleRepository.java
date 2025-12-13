package com.project.agriculturalblogapplication.Repositories;


import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(String roleName);

    boolean existsRoleByRoleName(String roleName);

    List<Role> findAllByRoleType(RoleType roleType);

    Optional<Role> findTopByRoleName(String roleName);

    Boolean existsByRoleName(String roleName);

    Boolean existsByRoleNameAndIdNot(String roleName, Long roleId);
}
