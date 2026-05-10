package com.hyperbrain.sopfc.common.application.outbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.common.domain.event.ExecutableDeletedEvent;
import com.hyperbrain.sopfc.common.domain.event.ExecutableStatusChangedEvent;
import com.hyperbrain.sopfc.common.infrastructure.adapter.out.kafka.KafkaEventPublisherAdapter;
import com.hyperbrain.sopfc.common.infrastructure.persistence.entity.OutboxEventEntity;
import com.hyperbrain.sopfc.common.infrastructure.persistence.repository.OutboxEventRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxEventRepository repository;
    private final KafkaEventPublisherAdapter kafkaPublisher;
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
            String topic = "hyperbrain." + currentEvent.getEventType().toLowerCase().replace("_", ".");
            String key = currentEvent.getAggregateId();
            String payload = currentEvent.getPayload();
            String sourceSystem = currentEvent.getSourceSystem();

            log.info("📢 [OUTBOX] Relaying event {} to Kafka topic {}. Aggregate: {}. Source: {}", 
                currentEvent.getEventType(), topic, key, sourceSystem);
            
            kafkaPublisher.publish(topic, key, payload, sourceSystem);
            
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
