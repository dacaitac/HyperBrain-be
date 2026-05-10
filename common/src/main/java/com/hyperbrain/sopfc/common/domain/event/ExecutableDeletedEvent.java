package com.hyperbrain.sopfc.common.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.List;
import java.util.UUID;

@Getter
public class ExecutableDeletedEvent extends ApplicationEvent {
    private final UUID executableId;
    private final String sourceSystem;
    private final List<MappingInfo> mappings;

    public ExecutableDeletedEvent(Object source, UUID executableId, String sourceSystem, List<MappingInfo> mappings) {
        super(source);
        this.executableId = executableId;
        this.sourceSystem = sourceSystem;
        this.mappings = mappings;
    }

    public record MappingInfo(String externalSystem, String externalId) {}
}
