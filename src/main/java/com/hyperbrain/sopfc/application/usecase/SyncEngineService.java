package com.hyperbrain.sopfc.application.usecase;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.port.out.EventPublisherPort;
import com.hyperbrain.sopfc.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.domain.port.out.SyncMappingRepositoryPort.SyncMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncEngineService {

    private final List<ExternalSyncPort> externalPorts;
    private final ExecutableRepositoryPort localRepo;
    private final SyncMappingRepositoryPort syncMappingRepo;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public void syncAll(UUID tenantId) {
        log.info("🚀 [SYNC-ALL] Starting global synchronization for tenant: {}", tenantId);
        externalPorts.forEach(port -> syncSystem(port, tenantId));
        log.info("✅ [SYNC-ALL] Global synchronization completed for tenant: {}", tenantId);
    }

    @Transactional
    public void processExternalUpdate(String system, String externalId, CoreExecutable updatedData) {
        log.info("🔄 [EXTERNAL-UPDATE] Processing update from {}. ExternalID: {}", system, externalId);
        
        syncMappingRepo.findByExternalId(externalId, system).ifPresentOrElse(
            mapping -> {
                localRepo.findByIdAndTenantId(mapping.executableId(), mapping.tenantId()).ifPresentOrElse(local -> {
                    String newChecksum = calculateChecksum(updatedData);
                    if (newChecksum.equals(mapping.lastKnownChecksum())) {
                        log.debug("⏭️ [EXTERNAL-UPDATE] No changes detected for item {}. Skipping.", externalId);
                        return;
                    }

                    log.info("📝 [EXTERNAL-UPDATE] Change detected in {}. Updating local executable: {}", system, local.getId());
                    CoreExecutable updatedLocal = local.toBuilder()
                            .name(updatedData.getName())
                            .status(updatedData.getStatus())
                            .isPlanned(updatedData.isPlanned())
                            .impact(updatedData.getImpact())
                            .startTime(updatedData.getStartTime())
                            .endTime(updatedData.getEndTime())
                            .executionProfile(updatedData.getExecutionProfile())
                            .build();
                    localRepo.save(updatedLocal);
                    updateMapping(mapping, updatedData);
                    
                    log.info("📢 [EXTERNAL-UPDATE] Publishing change event to other systems. Source: {}", system);
                    eventPublisher.publishExecutableStatusChanged(updatedLocal.getId(), updatedLocal.getTenantId(), updatedLocal.getStatus().name(), system);
                }, () -> log.error("❌ [EXTERNAL-UPDATE] Mapping exists but local executable not found: {}", mapping.executableId()));
            },
            () -> {
                log.info("➕ [EXTERNAL-UPDATE] No mapping found for {}. Creating new local entry.", externalId);
                // We use the tenantId from input or default
                UUID targetTenantId = (updatedData != null && updatedData.getTenantId() != null) ? 
                    updatedData.getTenantId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
                
                // If updatedData is null, we fetch it first
                CoreExecutable dataToSave = updatedData != null ? updatedData : 
                    externalPorts.stream()
                        .filter(p -> p.getSystemIdentifier().equals(system))
                        .findFirst()
                        .flatMap(p -> p.fetchById(externalId))
                        .map(ExternalSyncPort.ExternalSyncResult::executable)
                        .orElseThrow(() -> new RuntimeException("Could not fetch data for " + externalId));

                CoreExecutable saved = createLocalFromExternal(dataToSave, system, externalId, targetTenantId);
                
                log.info("📢 [EXTERNAL-UPDATE] Publishing creation event for new item: {}", saved.getId());
                eventPublisher.publishExecutableStatusChanged(saved.getId(), saved.getTenantId(), saved.getStatus().name(), system);
                return;
            }
        );
    }

    @Transactional
    public void processLocalDelete(UUID executableId, UUID tenantId) {
        log.info("🗑️ [LOCAL-DELETE] Initiating deletion for executable: {}", executableId);

        List<SyncMapping> mappings = syncMappingRepo.findAllByExecutableId(executableId);
        log.info("🔍 [LOCAL-DELETE] Found {} external mappings to clean up.", mappings.size());

        for (SyncMapping mapping : mappings) {
            externalPorts.stream()
                .filter(port -> port.getSystemIdentifier().equals(mapping.externalSystem()))
                .findFirst()
                .ifPresentOrElse(port -> {
                    log.info("🌐 [LOCAL-DELETE] Pushing deletion to {}: {}", mapping.externalSystem(), mapping.externalId());
                    port.pushDelete(mapping.externalId());
                }, () -> log.warn("⚠️ [LOCAL-DELETE] Adapter not found for system: {}", mapping.externalSystem()));
            
            log.info("🗑️ [LOCAL-DELETE] Removing mapping: {}", mapping.id());
            syncMappingRepo.deleteById(mapping.id());
        }

        log.info("🗑️ [LOCAL-DELETE] Deleting local record: {}", executableId);
        localRepo.delete(executableId, tenantId);
        log.info("✅ [LOCAL-DELETE] Deletion flow completed.");
    }

    private void syncSystem(ExternalSyncPort port, UUID tenantId) {
        String systemName = port.getSystemIdentifier();
        log.info("📥 [SYNC-SYSTEM] Pulling delta from: {}", systemName);

        try {
            List<ExternalSyncPort.ExternalSyncResult> externalResults = port.fetchDelta();
            log.info("📊 [SYNC-SYSTEM] {} returned {} items.", systemName, externalResults.size());

            for (ExternalSyncPort.ExternalSyncResult result : externalResults) {
                processExternalUpdate(systemName, result.externalId(), result.executable());
            }
        } catch (Exception e) {
            log.error("❌ [SYNC-SYSTEM] Error syncing with {}: {}", systemName, e.getMessage(), e);
        }
    }

    private String calculateChecksum(CoreExecutable executable) {
        return String.valueOf(java.util.Objects.hash(
                executable.getName(),
                executable.getStatus(),
                executable.isPlanned(),
                executable.getImpact(),
                executable.getStartTime(),
                executable.getEndTime(),
                executable.getExecutionProfile() != null ? executable.getExecutionProfile().getEnergyDrain() : 0,
                executable.getExecutionProfile() != null ? executable.getExecutionProfile().getMentalLoad() : 0
        ));
    }

    private void updateMapping(SyncMapping mapping, CoreExecutable extItem) {
        SyncMapping updatedMapping = new SyncMapping(
            mapping.id(),
            mapping.tenantId(),
            mapping.executableId(),
            mapping.externalSystem(),
            mapping.externalId(),
            calculateChecksum(extItem),
            OffsetDateTime.now(),
            "IN_SYNC"
        );
        syncMappingRepo.save(updatedMapping);
    }

    private CoreExecutable createLocalFromExternal(CoreExecutable extItem, String system, String extId, UUID tenantId) {
        CoreExecutable toSave = extItem.toBuilder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .build();
        CoreExecutable savedLocal = localRepo.save(toSave);
        
        SyncMapping mapping = new SyncMapping(
            null,
            tenantId,
            savedLocal.getId(),
            system,
            extId,
            calculateChecksum(extItem),
            OffsetDateTime.now(),
            "IN_SYNC"
        );
        syncMappingRepo.save(mapping);
        log.info("🔗 [MAPPING] Created link: Local {} <-> {} {}", savedLocal.getId(), system, extId);
        return savedLocal;
    }
}
