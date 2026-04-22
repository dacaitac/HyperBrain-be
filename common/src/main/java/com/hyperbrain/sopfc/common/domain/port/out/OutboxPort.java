package com.hyperbrain.sopfc.common.domain.port.out;

public interface OutboxPort {
    void saveEvent(String aggregateType, String aggregateId, String eventType, Object payload, String sourceSystem);
}
