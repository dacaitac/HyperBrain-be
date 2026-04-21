package com.hyperbrain.sopfc.sync.application.usecase;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort.SyncMapping;
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
    private final OutboxPort outboxPort;

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
                            .description(updatedData.getDescription())
                            .status(updatedData.getStatus())
                            .isPlanned(updatedData.isPlanned())
                            .impactScore(updatedData.getImpactScore())
                            .urgencyScore(updatedData.getUrgencyScore())
                            .effortScore(updatedData.getEffortScore())
                            .mentalLoad(updatedData.getMentalLoad())
                            .energyDrain(updatedData.getEnergyDrain())
                            .estimatedMinutes(updatedData.getEstimatedMinutes())
                            .startTime(updatedData.getStartTime())
                            .endTime(updatedData.getEndTime())
                            .build();
                    localRepo.save(updatedLocal);
                    updateMapping(mapping, updatedData);
                    
                    log.info("📢 [EXTERNAL-UPDATE] Saving change event to Outbox. Source: {}", system);
                    outboxPort.saveEvent(
                        updatedLocal.getTenantId(), 
                        "CORE_EXECUTABLE", 
                        updatedLocal.getId().toString(), 
                        "EXECUTABLE_STATUS_CHANGED", 
                        new StatusChangedPayload(updatedLocal.getStatus().name(), system)
                    );
                }, () -> log.error("❌ [EXTERNAL-UPDATE] Mapping exists but local executable not found: {}", mapping.executableId()));
            },
            () -> {
                log.info("➕ [EXTERNAL-UPDATE] No mapping found for {}. Creating new local entry.", externalId);
                UUID targetTenantId = (updatedData != null && updatedData.getTenantId() != null) ? 
                    updatedData.getTenantId() : UUID.fromString("00000000-0000-0000-0000-000000000001");
                
                CoreExecutable dataToSave = updatedData != null ? updatedData : 
                    externalPorts.stream()
                        .filter(p -> p.getSystemIdentifier().equals(system))
                        .findFirst()
                        .flatMap(p -> p.fetchById(externalId))
                        .map(ExternalSyncPort.ExternalSyncResult::executable)
                        .orElseThrow(() -> new RuntimeException("Could not fetch data for " + externalId));

                CoreExecutable saved = createLocalFromExternal(dataToSave, system, externalId, targetTenantId);
                
                log.info("📢 [EXTERNAL-UPDATE] Broadcasting creation to other systems for item: {}", saved.getId());
                // Disparar evento para que otros adaptadores (Apple) creen el recurso
                outboxPort.saveEvent(
                    saved.getTenantId(), 
                    "CORE_EXECUTABLE", 
                    saved.getId().toString(), 
                    "EXECUTABLE_CREATED", 
                    new StatusChangedPayload(saved.getStatus().name(), system)
                );
            }
        );
    }

    @Transactional
    public void processExternalDelete(String system, String externalId) {
        log.info("🗑️ [EXTERNAL-DELETE] Received delete from {}. ExternalID: {}", system, externalId);
        
        syncMappingRepo.findByExternalId(externalId, system).ifPresent(mapping -> {
            log.info("🗑️ [EXTERNAL-DELETE] Propagating delete for executable: {}", mapping.executableId());
            processLocalDelete(mapping.executableId(), mapping.tenantId());
        });
    }

    private record StatusChangedPayload(String newStatus, String sourceSystem) {}

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
                executable.getImpactScore(),
                executable.getUrgencyScore(),
                executable.getEstimatedMinutes(),
                executable.getStartTime(),
                executable.getEndTime(),
                executable.getEnergyDrain(),
                executable.getMentalLoad()
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
