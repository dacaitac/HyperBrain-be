package com.hyperbrain.sopfc.infrastructure.adapter.out.event;

import com.hyperbrain.sopfc.domain.port.out.EventPublisherPort;
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
    public void publishExecutableStatusChanged(UUID executableId, UUID tenantId, String newStatus, String sourceSystem) {
        ExecutableStatusChangedEvent event = new ExecutableStatusChangedEvent(this, executableId, tenantId, newStatus, sourceSystem);
        applicationEventPublisher.publishEvent(event);
    }
}
