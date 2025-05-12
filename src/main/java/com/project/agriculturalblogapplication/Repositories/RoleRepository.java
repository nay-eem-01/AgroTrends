package com.project.agriculturalblogapplication.Repositories;


import com.project.agriculturalblogapplication.Models.APP_ROLE;
import com.project.agriculturalblogapplication.Models.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Roles, Long> {

    Optional<Roles> findByRole(APP_ROLE appRole);
}
