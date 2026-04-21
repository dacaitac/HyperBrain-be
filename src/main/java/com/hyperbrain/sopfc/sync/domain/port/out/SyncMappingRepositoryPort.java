package com.hyperbrain.sopfc.sync.domain.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Port to manage the mapping between local Executables and external system IDs.
 */
public interface SyncMappingRepositoryPort {
    
    Optional<SyncMapping> findByExternalId(String externalId, String externalSystem);
    
    Optional<SyncMapping> findByExecutableId(UUID executableId, String externalSystem);
    
    java.util.List<SyncMapping> findAllByExecutableId(UUID executableId);
    
    java.util.List<SyncMapping> findAll();
    
    void save(SyncMapping mapping);
    
    void delete(SyncMapping mapping);
    
    void deleteById(UUID id);

    /** Domain model for the mapping. */
    record SyncMapping(
        UUID id,
        UUID tenantId,
        UUID executableId,
        String externalSystem,
        String externalId,
        String lastKnownChecksum,
        java.time.OffsetDateTime lastSyncedAt,
        String syncStatus
    ) {}
}
