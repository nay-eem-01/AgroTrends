package com.project.agriculturalblogapplication.repositories;


import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByName(String userName);

    boolean existsByName(String user1);

    boolean existsByEmail(String email);

    Optional<User> findTopByEmail(String email);

    Optional<User> findTopByEmailEqualsIgnoreCase(String username);

    Boolean existsByRolesIn(Set<Role> role);

    Boolean existsByMobileNumber(String mobileNumber);
}
