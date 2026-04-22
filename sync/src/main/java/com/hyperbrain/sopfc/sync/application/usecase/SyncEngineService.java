package com.hyperbrain.sopfc.sync.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.common.infrastructure.config.SyncContextHolder;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.sync.application.util.SyncUtils;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort.SyncMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncEngineService {

    private final List<ExternalSyncPort> externalPorts;
    private final ExecutableRepositoryPort localRepo;
    private final SyncMappingRepositoryPort syncMappingRepo;
    private final SyncPersistenceService syncPersistenceService;
    private final OutboxPort outboxPort;
    private final ObjectMapper objectMapper;

    @Transactional
    public void syncAll() {
        log.info("🔄 [SYNC-ENGINE] Starting full delta sync...");
        for (ExternalSyncPort port : externalPorts) {
            String system = port.getSystemIdentifier();
            try {
                SyncContextHolder.setSource(system);
                List<ExternalSyncPort.ExternalSyncResult> delta = port.fetchDelta();
                for (ExternalSyncPort.ExternalSyncResult extItem : delta) {
                    processExternalUpdate(system, extItem.externalId(), extItem.executable());
                }
            } finally {
                SyncContextHolder.clear();
            }
        }
    }

    @Transactional
    public void processExternalUpdate(String system, String externalId, CoreExecutable updatedData) {
        SyncContextHolder.setSource(system);
        syncMappingRepo.findByExternalId(externalId, system).ifPresentOrElse(
            mapping -> {
                localRepo.findById(mapping.executableId()).ifPresentOrElse(local -> {
                    String newChecksum = SyncUtils.calculateChecksum(updatedData);
                    if (newChecksum.equals(mapping.lastKnownChecksum())) {
                        log.info("⏭️ [EXTERNAL-UPDATE] No changes detected. Local Checksum matches Incoming ({}). Skipping.", newChecksum);
                        return;
                    }
                    log.info("🔄 [EXTERNAL-UPDATE] Change detected! Old: {}, New: {}", mapping.lastKnownChecksum(), newChecksum);

                    CoreExecutable updatedLocal = local.toBuilder()
                            .name(updatedData.getName())
                            .description(updatedData.getDescription())
                            .status(updatedData.getStatus())
                            .startTime(updatedData.getStartTime())
                            .applePriority(updatedData.getApplePriority())
                            .externalUrl(updatedData.getExternalUrl())
                            .build();
                    
                    syncPersistenceService.updateFullLinkAtomic(updatedLocal, updatedData, mapping);
                }, () -> log.warn("⚠️ [EXTERNAL-UPDATE] Local item missing: {}", mapping.executableId()));
            },
            () -> syncPersistenceService.createFullLinkAtomic(updatedData, system, externalId)
        );
    }

    @Transactional
    public void processLocalDelete(UUID executableId) {
        log.info("🗑️ [LOCAL-DELETE] Executable: {}", executableId);
        List<SyncMapping> mappings = syncMappingRepo.findAllByExecutableId(executableId);
        
        // Use a more explicit format for the payload
        String payload = mappings.stream()
                .map(m -> m.externalSystem() + ":" + m.externalId())
                .collect(java.util.stream.Collectors.joining(","));

        log.info("💾 [LOCAL-DELETE] Saving EXECUTABLE_DELETED to Outbox with mappings: {}", payload);
        outboxPort.saveEvent(
                "CORE_EXECUTABLE",
                executableId.toString(),
                "EXECUTABLE_DELETED",
                payload,
                "INTERNAL"
        );

        // Delete AFTER emitting event
        localRepo.delete(executableId);
        for (SyncMapping mapping : mappings) {
            syncMappingRepo.delete(mapping);
        }
    }

    @Transactional
    public void processExternalDelete(String system, String externalId) {
        log.info("🗑️ [EXTERNAL-DELETE-START] From {}: {}", system, externalId);
        SyncContextHolder.setSource(system);
        try {
            syncMappingRepo.findByExternalId(externalId, system).ifPresentOrElse(mapping -> {
                log.info("🗑️ [EXTERNAL-DELETE] Local record linked to {}: {}. Local ID: {}", 
                    system, externalId, mapping.executableId());
                
                List<SyncMapping> allMappings = syncMappingRepo.findAllByExecutableId(mapping.executableId());
                String payload = allMappings.stream()
                        .map(m -> m.externalSystem() + ":" + m.externalId())
                        .collect(java.util.stream.Collectors.joining(","));

                log.info("💾 [EXTERNAL-DELETE] Saving EXECUTABLE_DELETED to Outbox with mappings: {}", payload);
                outboxPort.saveEvent(
                        "CORE_EXECUTABLE",
                        mapping.executableId().toString(),
                        "EXECUTABLE_DELETED",
                        payload,
                        system
                );

                // Delete AFTER emitting event
                localRepo.delete(mapping.executableId());
                syncMappingRepo.delete(mapping);
            }, () -> log.warn("⚠️ [EXTERNAL-DELETE] No mapping found for external ID {} in system {}", externalId, system));
        } finally {
            // Cleared by caller
        }
    }
}
