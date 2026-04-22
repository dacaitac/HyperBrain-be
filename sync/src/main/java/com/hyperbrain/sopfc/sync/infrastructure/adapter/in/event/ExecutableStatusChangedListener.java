package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.event;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.sync.application.util.SyncUtils;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.common.domain.event.ExecutableStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutableStatusChangedListener {

    private final List<ExternalSyncPort> externalPorts;
    private final ExecutableRepositoryPort localRepo;
    private final SyncMappingRepositoryPort syncMappingRepo;

    @EventListener
    public void onExecutableStatusChanged(ExecutableStatusChangedEvent event) {
        log.info("🔔 [EVENT-RECEPTION] Status changed for {} from source {}", 
            event.getExecutableId(), event.getSourceSystem());
        
        localRepo.findById(event.getExecutableId()).ifPresentOrElse(executable -> {
            externalPorts.forEach(port -> {
                String system = port.getSystemIdentifier();
                
                // CRITICAL: system must not be null and must not be the source of the event
                if (system == null || system.equals(event.getSourceSystem())) {
                    log.debug("⏭️ [PROPAGATION] Skipping system: {}", system);
                    return;
                }

                if ("APPLE_CALENDAR".equals(system) && !executable.isPlanned()) {
                    return;
                }

                log.info("🌐 [PROPAGATION] Syncing change to {}: ID {}", system, executable.getId());
                syncMappingRepo.findByExecutableId(executable.getId(), system)
                        .ifPresentOrElse(
                            mapping -> {
                                try {
                                    port.pushUpdate(executable, mapping.externalId());
                                } catch (Exception e) {
                                    log.error("❌ [PROPAGATION] Error updating {}: {}", system, e.getMessage());
                                }
                            },
                            () -> {
                                try {
                                    log.info("➕ [PROPAGATION] Pushing NEW item to {}: {}", system, executable.getName());
                                    String extId = port.pushCreate(executable);
                                    if (extId != null) {
                                        log.info("✅ [PROPAGATION] Created in {}. External ID: {}", system, extId);
                                        if (syncMappingRepo.findByExecutableId(executable.getId(), system).isEmpty()) {
                                            saveNewMapping(executable, system, extId);
                                        }
                                    } else {
                                        log.warn("⚠️ [PROPAGATION] Port {} returned null ID for creation.", system);
                                    }
                                } catch (Exception e) {
                                    log.error("❌ [PROPAGATION] Exception creating in {}: {}", system, e.getMessage(), e);
                                }
                            }
                        );
            });
        }, () -> log.warn("⚠️ [EVENT-RECEPTION] Executable {} not found", event.getExecutableId()));
    }

    private void saveNewMapping(CoreExecutable executable, String system, String extId) {
        SyncMappingRepositoryPort.SyncMapping mapping = new SyncMappingRepositoryPort.SyncMapping(
            null,
            executable.getId(),
            system,
            extId,
            SyncUtils.calculateChecksum(executable),
            OffsetDateTime.now(),
            "IN_SYNC"
        );
        try {
            syncMappingRepo.save(mapping);
            log.info("✅ [LISTENER-MAPPING] Established link with {}: {}", system, extId);
        } catch (Exception ignored) {}
    }
}
