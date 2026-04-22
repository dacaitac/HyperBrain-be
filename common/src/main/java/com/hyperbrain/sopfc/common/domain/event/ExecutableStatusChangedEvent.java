package com.hyperbrain.sopfc.common.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class ExecutableStatusChangedEvent extends ApplicationEvent {
    private final UUID executableId;
    private final String newStatus;
    private final String sourceSystem;

    public ExecutableStatusChangedEvent(Object source, UUID executableId, String newStatus, String sourceSystem) {
        super(source);
        this.executableId = executableId;
        this.newStatus = newStatus;
        this.sourceSystem = sourceSystem;
    }
}
