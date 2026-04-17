package com.hyperbrain.sopfc.infrastructure.adapter.out.persistence.repository;

import com.hyperbrain.sopfc.infrastructure.adapter.out.persistence.entity.SyncMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSyncMappingRepository extends JpaRepository<SyncMappingEntity, UUID> {

    Optional<SyncMappingEntity> findByExternalIdAndExternalSystem(String externalId, String externalSystem);

    Optional<SyncMappingEntity> findByExecutableIdAndExternalSystem(UUID executableId, String externalSystem);

    java.util.List<SyncMappingEntity> findAllByExecutableId(UUID executableId);
}
