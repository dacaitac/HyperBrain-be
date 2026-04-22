package com.hyperbrain.sopfc.common.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.common.infrastructure.persistence.entity.OutboxEventEntity;
import com.hyperbrain.sopfc.common.infrastructure.persistence.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxPort {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveEvent(String aggregateType, String aggregateId, String eventType, Object payload, String sourceSystem) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEventEntity entity = OutboxEventEntity.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .sourceSystem(sourceSystem)
                    .build();
            repository.save(entity);
            log.debug("💾 [OUTBOX-SAVE] Saved {} for {}: {} (Source: {})", eventType, aggregateType, aggregateId, sourceSystem);
        } catch (Exception e) {
            log.error("❌ [OUTBOX-ERROR] Failed to serialize event payload: {}", e.getMessage());
            throw new RuntimeException("Outbox serialization error", e);
        }
    }
}
