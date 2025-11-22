package com.project.agriculturalblogapplication.Repositories;


import com.project.agriculturalblogapplication.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepositories extends JpaRepository<Users, Long> {
    Optional<Users> findByUserEmail(String email);
    Optional<Users> findByUserName(String userName);

    boolean existsByUserName(String user1);

    boolean existsByUserEmail(String email);
}
