package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    List<Tag> findByNameStartingWith(String prefix, Pageable pageable);
}
