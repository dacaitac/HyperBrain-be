package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.common.application.outbox.OutboxScheduler;
import com.hyperbrain.sopfc.common.domain.event.ExecutableDeletedEvent;
import com.hyperbrain.sopfc.common.domain.event.ExecutableStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncKafkaConsumer {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"hyperbrain.executable.status.changed", "hyperbrain.executable.created"}, groupId = "sync-group")
    public void onExecutableStatusChanged(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(value = "source_system", required = false) byte[] sourceSystemBytes) {
        
        String sourceSystem = sourceSystemBytes != null ? new String(sourceSystemBytes, StandardCharsets.UTF_8) : null;
        log.info("📥 [KAFKA-CONSUME] Received status changed event for key: {} from source: {}", key, sourceSystem);
        
        try {
            // Clean up string payload if needed
            String rawPayload = payload;
            if (rawPayload.startsWith("\"") && rawPayload.endsWith("\"") && rawPayload.length() > 2) {
                rawPayload = rawPayload.substring(1, rawPayload.length() - 1).replace("\\\"", "\"");
            }

            OutboxScheduler.StatusChangedPayload statusPayload = objectMapper.readValue(rawPayload, OutboxScheduler.StatusChangedPayload.class);
            
            ExecutableStatusChangedEvent event = new ExecutableStatusChangedEvent(
                    this,
                    UUID.fromString(key),
                    statusPayload.getNewStatus(),
                    sourceSystem != null ? sourceSystem : statusPayload.getSourceSystem()
            );

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("❌ [KAFKA-CONSUME] Error processing Kafka event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "hyperbrain.executable.deleted", groupId = "sync-group")
    public void onExecutableDeleted(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(value = "source_system", required = false) byte[] sourceSystemBytes) {

        String sourceSystem = sourceSystemBytes != null ? new String(sourceSystemBytes, StandardCharsets.UTF_8) : null;
        log.info("📥 [KAFKA-CONSUME] Received deletion event for key: {} from source: {}", key, sourceSystem);

        try {
            // Clean up string payload if needed
            String rawPayload = payload;
            if (rawPayload.startsWith("\"") && rawPayload.endsWith("\"") && rawPayload.length() > 2) {
                rawPayload = rawPayload.substring(1, rawPayload.length() - 1).replace("\\\"", "\"");
            }

            // The payload for deletion is a comma-separated string of SYSTEM:ID
            List<ExecutableDeletedEvent.MappingInfo> mappings = Arrays.stream(rawPayload.split(","))
                    .map(s -> s.split(":"))
                    .filter(parts -> parts.length == 2)
                    .map(parts -> new ExecutableDeletedEvent.MappingInfo(parts[0], parts[1]))
                    .collect(Collectors.toList());

            ExecutableDeletedEvent event = new ExecutableDeletedEvent(
                    this,
                    UUID.fromString(key),
                    sourceSystem,
                    mappings
            );

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("❌ [KAFKA-CONSUME] Error processing Deletion Kafka event: {}", e.getMessage());
        }
    }
}
