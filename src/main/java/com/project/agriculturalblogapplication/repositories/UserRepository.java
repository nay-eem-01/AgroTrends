package com.project.agriculturalblogapplication.repositories;


import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<User> findByGoogleSubject(String googleSubject);

    /**
     * Admin user search: {@code query} (lower case, may be empty) inside the name or e-mail; {@code banned} null for
     * everyone, true for banned accounts only, false for the rest.
     */
    @Query("""
            select u from User u
            where (locate(:query, lower(u.name)) > 0 or locate(:query, lower(u.email)) > 0)
              and (:banned is null or (:banned = true and u.bannedAt is not null) or (:banned = false and u.bannedAt is null))""")
    Page<User> search(@Param("query") String query, @Param("banned") Boolean banned, Pageable pageable);
}
