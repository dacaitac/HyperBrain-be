package com.hyperbrain.sopfc.common.application.outbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.common.domain.event.ExecutableStatusChangedEvent;
import com.hyperbrain.sopfc.common.infrastructure.persistence.entity.OutboxEventEntity;
import com.hyperbrain.sopfc.common.infrastructure.persistence.repository.OutboxEventRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxEventRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processOutbox() {
        List<OutboxEventEntity> pendingEvents = repository.findByProcessedFalseOrderByOccurredAtAsc();
        if (pendingEvents.isEmpty()) return;

        for (OutboxEventEntity event : pendingEvents) {
            try {
                if ("EXECUTABLE_STATUS_CHANGED".equals(event.getEventType())) {
                    String rawPayload = event.getPayload();
                    // Limpieza profunda de escapes de JSON si existen (común en H2/Postgres JSON types)
                    if (rawPayload.startsWith("\"") && rawPayload.endsWith("\"") && rawPayload.length() > 2) {
                        rawPayload = rawPayload.substring(1, rawPayload.length() - 1).replace("\\\"", "\"");
                    }
                    
                    StatusChangedPayload payload = objectMapper.readValue(rawPayload, StatusChangedPayload.class);
                    
                    ExecutableStatusChangedEvent domainEvent = new ExecutableStatusChangedEvent(
                            this,
                            UUID.fromString(event.getAggregateId()),
                            event.getTenantId(),
                            payload.getNewStatus(),
                            payload.getSourceSystem()
                    );
                    eventPublisher.publishEvent(domainEvent);
                }
                
                event.setProcessed(true);
                repository.save(event);
                log.info("✅ [OUTBOX-PROCESS] Published: {} - {}", event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("❌ [OUTBOX-PROCESS] Error deserializing event {}: {}", event.getId(), e.getMessage());
                // Fallback: Si el payload viene con comillas extra por error de driver
                try {
                    String cleanPayload = event.getPayload().replace("\\\"", "\"");
                    if (cleanPayload.startsWith("\"") && cleanPayload.endsWith("\"")) {
                        cleanPayload = cleanPayload.substring(1, cleanPayload.length() - 1);
                    }
                    StatusChangedPayload payload = objectMapper.readValue(cleanPayload, StatusChangedPayload.class);
                    // ... procesar igual
                } catch (Exception ignored) {}
            }
        }
    }

    @Getter
    public static class StatusChangedPayload {
        private final String newStatus;
        private final String sourceSystem;

        @JsonCreator
        public StatusChangedPayload(
                @JsonProperty("newStatus") String newStatus,
                @JsonProperty("sourceSystem") String sourceSystem) {
            this.newStatus = newStatus;
            this.sourceSystem = sourceSystem;
        }
    }
}
