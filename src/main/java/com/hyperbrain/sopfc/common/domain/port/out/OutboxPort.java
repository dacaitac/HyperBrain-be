package com.hyperbrain.sopfc.common.domain.port.out;

import java.util.UUID;

public interface OutboxPort {
    void saveEvent(UUID tenantId, String aggregateType, String aggregateId, String eventType, Object payload);
}
