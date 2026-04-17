package com.hyperbrain.sopfc.domain.port.out;

import java.util.UUID;

public interface EventPublisherPort {
    void publishExecutableStatusChanged(UUID executableId, UUID tenantId, String newStatus, String sourceSystem);
}
