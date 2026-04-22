package com.hyperbrain.sopfc.common.application.outbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.common.domain.event.ExecutableDeletedEvent;
import com.hyperbrain.sopfc.common.domain.event.ExecutableStatusChangedEvent;
import com.hyperbrain.sopfc.common.infrastructure.persistence.entity.OutboxEventEntity;
import com.hyperbrain.sopfc.common.infrastructure.persistence.repository.OutboxEventRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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
    public void processOutbox() {
        List<OutboxEventEntity> pendingEvents = repository.findByProcessedFalseOrderByOccurredAtAsc();
        if (pendingEvents.isEmpty()) return;
        for (OutboxEventEntity event : pendingEvents) {
            processEvent(event);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEvent(OutboxEventEntity event) {
        OutboxEventEntity currentEvent = repository.findById(event.getId()).orElse(null);
        if (currentEvent == null || currentEvent.isProcessed()) return;

        try {
            if ("EXECUTABLE_STATUS_CHANGED".equals(currentEvent.getEventType()) || "EXECUTABLE_CREATED".equals(currentEvent.getEventType())) {
                String rawPayload = currentEvent.getPayload();
                if (rawPayload.startsWith("\"") && rawPayload.endsWith("\"") && rawPayload.length() > 2) {
                    rawPayload = rawPayload.substring(1, rawPayload.length() - 1).replace("\\\"", "\"");
                }
                
                StatusChangedPayload payload = objectMapper.readValue(rawPayload, StatusChangedPayload.class);
                
                String sourceSystem = currentEvent.getSourceSystem();
                if (sourceSystem == null) sourceSystem = payload.getSourceSystem();

                ExecutableStatusChangedEvent domainEvent = new ExecutableStatusChangedEvent(
                        this,
                        UUID.fromString(currentEvent.getAggregateId()),
                        payload.getNewStatus(),
                        sourceSystem
                );
                
                log.info("📢 [OUTBOX] Publishing {} for aggregate {}. Source: {}", 
                    currentEvent.getEventType(), currentEvent.getAggregateId(), sourceSystem);
                eventPublisher.publishEvent(domainEvent);
            } else if ("EXECUTABLE_DELETED".equals(currentEvent.getEventType())) {
                String sourceSystem = currentEvent.getSourceSystem();
                String rawPayload = currentEvent.getPayload();
                
                // Clean up string payload if it was saved with quotes (e.g. "\"NOTION:id\"")
                if (rawPayload != null && rawPayload.startsWith("\"") && rawPayload.endsWith("\"") && rawPayload.length() > 2) {
                    rawPayload = rawPayload.substring(1, rawPayload.length() - 1);
                }

                List<ExecutableDeletedEvent.MappingInfo> mappings = new java.util.ArrayList<>();
                if (rawPayload != null && !rawPayload.isBlank()) {
                    // It could be double escaped or have extra slashes if it was treated as a single JSON string
                    rawPayload = rawPayload.replace("\\\"", ""); 

                    for (String part : rawPayload.split(",")) {
                        String[] pair = part.split(":", 2);
                        if (pair.length == 2) {
                            mappings.add(new ExecutableDeletedEvent.MappingInfo(pair[0].trim(), pair[1].trim()));
                        }
                    }
                }

                ExecutableDeletedEvent domainEvent = new ExecutableDeletedEvent(
                        this,
                        UUID.fromString(currentEvent.getAggregateId()),
                        sourceSystem,
                        mappings
                );
                
                log.info("📢 [OUTBOX] Publishing EXECUTABLE_DELETED for aggregate {}. Source: {}. Mappings: {}", 
                    currentEvent.getAggregateId(), sourceSystem, mappings.size());
                eventPublisher.publishEvent(domainEvent);
            }
            
            currentEvent.setProcessed(true);
            repository.saveAndFlush(currentEvent);
            log.info("✅ [OUTBOX-PROCESS] Event {} marked as processed and flushed.", currentEvent.getId());
        } catch (Exception e) {
            log.error("❌ [OUTBOX-PROCESS] Error processing event {}: {}", currentEvent.getId(), e.getMessage());
            currentEvent.setProcessed(true); // Mark as processed anyway to avoid infinite loops on bad data
            repository.saveAndFlush(currentEvent);
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
