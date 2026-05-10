package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.event;

import com.hyperbrain.sopfc.common.domain.event.ExecutableDeletedEvent;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutableDeletedListener {

    private final List<ExternalSyncPort> externalPorts;
    private final SyncMappingRepositoryPort syncMappingRepo;

    @EventListener
    public void onExecutableDeleted(ExecutableDeletedEvent event) {
        log.info("🗑️ [EVENT-RECEPTION] Executable deleted: {} from source {}", 
            event.getExecutableId(), event.getSourceSystem());
        
        externalPorts.forEach(port -> {
            String system = port.getSystemIdentifier();
            
            if (system != null && system.equals(event.getSourceSystem())) {
                log.debug("⏭️ [PROPAGATION] Skipping origin system: {}", system);
                return;
            }

            event.getMappings().stream()
                    .filter(m -> m.externalSystem().equals(system))
                    .findFirst()
                    .ifPresent(m -> {
                        try {
                            log.info("🌐 [PROPAGATION] Deleting from {}: External ID {}", system, m.externalId());
                            port.pushDelete(m.externalId());
                        } catch (Exception e) {
                            log.error("❌ [PROPAGATION] Error deleting from {}: {}", system, e.getMessage());
                        }
                    });
        });
    }
}
