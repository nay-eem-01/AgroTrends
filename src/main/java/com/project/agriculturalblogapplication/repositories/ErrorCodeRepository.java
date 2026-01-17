package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.ErrorCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ErrorCodeRepository extends JpaRepository<ErrorCodeEntity, Long>, JpaSpecificationExecutor<ErrorCodeEntity> {

    Optional<ErrorCodeEntity> findTopByInternalCode(String internalCode);

    Boolean existsByInternalCode(String internalCode);

    Boolean existsByInternalCodeAndIdNot(String internalCode, Long id);
}
