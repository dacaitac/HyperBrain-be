package com.hyperbrain.sopfc.infrastructure.adapter.out.persistence.repository;

import com.hyperbrain.sopfc.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCoreExecutableRepository extends JpaRepository<CoreExecutableEntity, UUID> {
    Optional<CoreExecutableEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}