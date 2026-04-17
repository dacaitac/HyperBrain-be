package com.hyperbrain.sopfc.infrastructure.adapter.in.event;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.infrastructure.adapter.out.event.ExecutableStatusChangedEvent;
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
        log.info("🔔 [EVENT-RECEPTION] Status changed for executable {} from source {}", 
            event.getExecutableId(), event.getSourceSystem());
        
        localRepo.findByIdAndTenantId(event.getExecutableId(), event.getTenantId()).ifPresentOrElse(executable -> {
            log.debug("📄 [EVENT-RECEPTION] Data found: name='{}', status={}", executable.getName(), executable.getStatus());
            
            externalPorts.forEach(port -> {
                String system = port.getSystemIdentifier();
                
                if (system.equals(event.getSourceSystem())) {
                    log.debug("⏭️ [PROPAGATION] Skipping origin system: {}", system);
                    return;
                }

                // Lógica de filtrado de Calendario vs Reminders
                if (system.equals("APPLE_CALENDAR")) {
                    if (!executable.isPlanned()) {
                        log.debug("⏭️ [PROPAGATION] Skipping APPLE_CALENDAR for non-planned executable: {}", executable.getId());
                        return;
                    }
                }

                log.info("🌐 [PROPAGATION] Syncing change to {}: ID {}", system, executable.getId());
                syncMappingRepo.findByExecutableId(executable.getId(), system)
                        .ifPresentOrElse(
                            mapping -> {
                                log.info("🔄 [PROPAGATION] Updating existing mapping in {}: {}", system, mapping.externalId());
                                port.pushUpdate(executable, mapping.externalId());
                            },
                            () -> {
                                log.info("➕ [PROPAGATION] Creating new entry in {}.", system);
                                try {
                                    String extId = port.pushCreate(executable);
                                    if (extId != null) {
                                        saveNewMapping(executable, system, extId);
                                        log.info("✅ [PROPAGATION] Link established with {} (ExtID: {})", system, extId);
                                    }
                                } catch (Exception e) {
                                    log.error("❌ [PROPAGATION] Error pushing to {}: {}", system, e.getMessage());
                                }
                            }
                        );
            });
        }, () -> log.warn("⚠️ [EVENT-RECEPTION] Executable {} not found for tenant {}", event.getExecutableId(), event.getTenantId()));
    }

    private void saveNewMapping(CoreExecutable executable, String system, String extId) {
        SyncMappingRepositoryPort.SyncMapping mapping = new SyncMappingRepositoryPort.SyncMapping(
            null,
            executable.getTenantId(),
            executable.getId(),
            system,
            extId,
            "NEW_FROM_EVENT",
            OffsetDateTime.now(),
            "IN_SYNC"
        );
        syncMappingRepo.save(mapping);
    }
}
