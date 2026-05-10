package com.hyperbrain.sopfc.common.infrastructure.adapter.out.event;

import com.hyperbrain.sopfc.common.domain.event.ExecutableDeletedEvent;
import com.hyperbrain.sopfc.common.domain.event.ExecutableStatusChangedEvent;

import com.hyperbrain.sopfc.common.domain.port.out.EventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishExecutableStatusChanged(UUID executableId, String newStatus, String sourceSystem) {
        ExecutableStatusChangedEvent event = new ExecutableStatusChangedEvent(this, executableId, newStatus, sourceSystem);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishExecutableDeleted(UUID executableId, String sourceSystem, java.util.List<com.hyperbrain.sopfc.common.domain.event.ExecutableDeletedEvent.MappingInfo> mappings) {
        ExecutableDeletedEvent event = new ExecutableDeletedEvent(this, executableId, sourceSystem, mappings);
        applicationEventPublisher.publishEvent(event);
    }
}
