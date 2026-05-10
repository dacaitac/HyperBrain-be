package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.event;

import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.common.infrastructure.config.SyncContextHolder;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SyncTriggerAspect {

    private final OutboxPort outboxPort;

    /**
     * Intercepts any call to ExecutableRepositoryPort.save()
     * This ensures that whenever an executable is saved (by any module), 
     * a sync event is recorded in the Outbox.
     */
    @AfterReturning(
        pointcut = "execution(* com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort.save(..))",
        returning = "savedExecutable"
    )
    public void afterExecutableSave(Object savedExecutable) {
        if (!(savedExecutable instanceof CoreExecutable executable)) return;

        String source = SyncContextHolder.getSource();
        if (source == null || source.isBlank()) source = "LOCAL_SYSTEM";
        
        log.info("📡 [SYNC-ASPECT] Save detected for {}. Source: {}. Emitting event to Outbox.", 
            executable.getId(), source);

        // We use EXECUTABLE_STATUS_CHANGED as a generic "something changed" event
        // that the Sync module knows how to handle.
        outboxPort.saveEvent(
            "CORE_EXECUTABLE",
            executable.getId().toString(),
            "EXECUTABLE_STATUS_CHANGED",
            new StatusChangedPayload(executable.getStatus().name(), source),
            source
        );
    }

    private record StatusChangedPayload(String newStatus, String sourceSystem) {}
}
