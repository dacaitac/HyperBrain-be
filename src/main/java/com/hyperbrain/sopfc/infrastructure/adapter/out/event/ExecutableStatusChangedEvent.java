package com.hyperbrain.sopfc.infrastructure.adapter.out.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class ExecutableStatusChangedEvent extends ApplicationEvent {
    private final UUID executableId;
    private final UUID tenantId;
    private final String newStatus;
    private final String sourceSystem;

    public ExecutableStatusChangedEvent(Object source, UUID executableId, UUID tenantId, String newStatus, String sourceSystem) {
        super(source);
        this.executableId = executableId;
        this.tenantId = tenantId;
        this.newStatus = newStatus;
        this.sourceSystem = sourceSystem;
    }
}
