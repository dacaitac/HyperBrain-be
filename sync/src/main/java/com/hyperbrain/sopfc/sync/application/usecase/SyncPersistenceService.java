package com.hyperbrain.sopfc.sync.application.usecase;

import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.common.infrastructure.config.SyncContextHolder;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.sync.application.util.SyncUtils;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort.SyncMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncPersistenceService {

    private final ExecutableRepositoryPort localRepo;
    private final SyncMappingRepositoryPort syncMappingRepo;
    private final OutboxPort outboxPort;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CoreExecutable createFullLinkAtomic(CoreExecutable extItem, String system, String extId) {
        SyncContextHolder.setSource(system);
        
        // Normalizamos el ID externo antes de guardarlo
        String normalizedExtId = "NOTION".equals(system) ? SyncUtils.normalizeNotionId(extId) : extId;
        
        UUID newLocalId = UUID.randomUUID();
        CoreExecutable toSave = extItem.toBuilder().id(newLocalId).build();
        
        log.info("💾 [DB-ATOMIC] Step 1: Saving Executable {}", newLocalId);
        CoreExecutable saved = localRepo.save(toSave);
        
        SyncMapping mapping = new SyncMapping(
            UUID.randomUUID(),
            newLocalId,
            system,
            normalizedExtId,
            SyncUtils.calculateChecksum(extItem),
            OffsetDateTime.now(),
            "IN_SYNC"
        );
        
        log.info("💾 [DB-ATOMIC] Step 2: Saving Mapping for {} (ExtID: {})", system, normalizedExtId);
        syncMappingRepo.save(mapping);

        log.info("📢 [DB-ATOMIC] Step 3: Emitting EXECUTABLE_CREATED to Outbox");
        outboxPort.saveEvent(
            "CORE_EXECUTABLE",
            newLocalId.toString(),
            "EXECUTABLE_CREATED",
            "{\"newStatus\":\"" + saved.getStatus() + "\",\"sourceSystem\":\"" + system + "\"}",
            system
        );
        
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateFullLinkAtomic(CoreExecutable local, CoreExecutable updatedData, SyncMapping mapping) {
        SyncContextHolder.setSource(mapping.externalSystem());
        
        log.info("💾 [DB-ATOMIC] Updating Executable {}", local.getId());
        localRepo.save(local);
        
        SyncMapping updatedMapping = new SyncMapping(
            mapping.id(),
            mapping.executableId(),
            mapping.externalSystem(),
            mapping.externalId(),
            SyncUtils.calculateChecksum(updatedData),
            OffsetDateTime.now(),
            "IN_SYNC"
        );
        syncMappingRepo.save(updatedMapping);

        log.info("📢 [DB-ATOMIC] Emitting EXECUTABLE_STATUS_CHANGED to Outbox");
        outboxPort.saveEvent(
            "CORE_EXECUTABLE",
            local.getId().toString(),
            "EXECUTABLE_STATUS_CHANGED",
            "{\"newStatus\":\"" + local.getStatus() + "\",\"sourceSystem\":\"" + mapping.externalSystem() + "\"}",
            mapping.externalSystem()
        );
    }
}
