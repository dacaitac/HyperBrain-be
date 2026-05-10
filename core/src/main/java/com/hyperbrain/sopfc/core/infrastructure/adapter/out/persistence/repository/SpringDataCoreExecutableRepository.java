package com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository;

import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCoreExecutableRepository extends JpaRepository<CoreExecutableEntity, UUID> {
    java.util.List<CoreExecutableEntity> findByNameIgnoreCase(String name);
    java.util.List<CoreExecutableEntity> findByNameIgnoreCaseAndStartTime(String name, java.time.OffsetDateTime startTime);
}