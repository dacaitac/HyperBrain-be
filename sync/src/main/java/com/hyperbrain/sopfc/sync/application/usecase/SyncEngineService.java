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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncEngineService {

    private final List<ExternalSyncPort> externalPorts;
    private final ExecutableRepositoryPort localRepo;
    private final SyncMappingRepositoryPort syncMappingRepo;
    private final SyncPersistenceService syncPersistenceService;
    private final OutboxPort outboxPort;

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
        
        // Normalize external ID for consistent lookup
        String normalizedId = "NOTION".equals(system) ? SyncUtils.normalizeNotionId(externalId) : externalId;

        syncMappingRepo.findByExternalId(normalizedId, system).ifPresentOrElse(
            mapping -> updateExistingMapping(mapping, updatedData, system, normalizedId),
            () -> searchAndLinkCandidate(updatedData, system, normalizedId)
        );
    }

    private void updateExistingMapping(SyncMapping mapping, CoreExecutable updatedData, String system, String normalizedId) {
        localRepo.findById(mapping.executableId()).ifPresentOrElse(local -> {
            CoreExecutable updatedLocal = applyExternalChanges(local, updatedData);

            String newChecksum = SyncUtils.calculateChecksum(updatedLocal);
            if (newChecksum.equals(mapping.lastKnownChecksum())) {
                log.info("⏭️ [EXTERNAL-UPDATE] No functional changes for {} ({}). Checksum match. Skipping.", system, normalizedId);
                return;
            }
            
            log.info("🔄 [EXTERNAL-UPDATE] Change detected in {} ({}). Old Checksum: {}, New: {}", 
                system, normalizedId, mapping.lastKnownChecksum(), newChecksum);
            
            syncPersistenceService.updateFullLinkAtomic(updatedLocal, updatedLocal, mapping);
        }, () -> {
            log.warn("⚠️ [EXTERNAL-UPDATE] Local item missing for mapping: {}. Re-creating link.", mapping.executableId());
            syncPersistenceService.createFullLinkAtomic(updatedData, system, normalizedId);
        });
    }

    private void searchAndLinkCandidate(CoreExecutable updatedData, String system, String normalizedId) {
        log.info("🔍 [EXTERNAL-UPDATE] Mapping not found for {} in {}. Searching for existing candidates...", normalizedId, system);

        List<CoreExecutable> candidates = localRepo.findByName(updatedData.getName());
        
        Optional<CoreExecutable> match = candidates.stream()
            .filter(c -> !syncMappingRepo.findByExecutableId(c.getId(), "NOTION").isEmpty())
            .findFirst();

        if (match.isPresent()) {
            CoreExecutable candidate = match.get();
            log.info("🤝 [DEDUPLICATION] Matched existing Notion task '{}' (ID: {}). Linking to {} ID: {}", 
                updatedData.getName(), candidate.getId(), system, normalizedId);
            
            syncPersistenceService.linkExistingAtomic(candidate, system, normalizedId);
            
            syncMappingRepo.findByExternalId(normalizedId, system).ifPresent(newMapping -> 
                syncPersistenceService.updateFullLinkAtomic(candidate, updatedData, newMapping)
            );
        } else {
            log.info("➕ [EXTERNAL-UPDATE] No suitable candidate found. Creating new link for: {}", updatedData.getName());
            syncPersistenceService.createFullLinkAtomic(updatedData, system, normalizedId);
        }
    }

    private CoreExecutable applyExternalChanges(CoreExecutable local, CoreExecutable external) {
        return local.toBuilder()
                .name(external.getName())
                .description(external.getDescription())
                .status(external.getStatus())
                .startTime(external.getStartTime())
                .endTime(external.getEndTime())
                .isPlanned(external.isPlanned())
                .impactScore(external.getImpactScore())
                .energyDrain(external.getEnergyDrain())
                .mentalLoad(external.getMentalLoad())
                .estimatedMinutes(external.getEstimatedMinutes())
                .applePriority(external.getApplePriority())
                .externalUrl(external.getExternalUrl())
                .sourceCalendar(external.getSourceCalendar())
                .alarms(external.getAlarms())
                .recurrence(external.getRecurrence())
                .build();
    }

    @Transactional
    public void processLocalDelete(UUID executableId) {
        log.info("🗑️ [LOCAL-DELETE] Executable: {}", executableId);
        List<SyncMapping> mappings = syncMappingRepo.findAllByExecutableId(executableId);
        String payload = generateDeletionPayload(mappings);

        log.info("💾 [LOCAL-DELETE] Saving EXECUTABLE_DELETED to Outbox with mappings: {}", payload);
        outboxPort.saveEvent("CORE_EXECUTABLE", executableId.toString(), "EXECUTABLE_DELETED", payload, "INTERNAL");

        localRepo.delete(executableId);
        mappings.forEach(syncMappingRepo::delete);
    }

    @Transactional
    public void processExternalDelete(String system, String externalId) {
        log.info("🗑️ [EXTERNAL-DELETE-START] From {}: {}", system, externalId);
        SyncContextHolder.setSource(system);
        
        syncMappingRepo.findByExternalId(externalId, system).ifPresentOrElse(mapping -> {
            log.info("🗑️ [EXTERNAL-DELETE] Local record linked to {}: {}. Local ID: {}", 
                system, externalId, mapping.executableId());
            
            List<SyncMapping> allMappings = syncMappingRepo.findAllByExecutableId(mapping.executableId());
            String payload = generateDeletionPayload(allMappings);

            log.info("💾 [EXTERNAL-DELETE] Saving EXECUTABLE_DELETED to Outbox with mappings: {}", payload);
            outboxPort.saveEvent("CORE_EXECUTABLE", mapping.executableId().toString(), "EXECUTABLE_DELETED", payload, system);

            localRepo.delete(mapping.executableId());
            syncMappingRepo.delete(mapping);
        }, () -> log.warn("⚠️ [EXTERNAL-DELETE] No mapping found for external ID {} in system {}", externalId, system));
    }

    private String generateDeletionPayload(List<SyncMapping> mappings) {
        return mappings.stream()
                .map(m -> m.externalSystem() + ":" + m.externalId())
                .collect(Collectors.joining(","));
    }
}

