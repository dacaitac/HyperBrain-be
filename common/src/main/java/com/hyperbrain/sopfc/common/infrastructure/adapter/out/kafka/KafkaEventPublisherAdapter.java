package com.hyperbrain.sopfc.common.infrastructure.adapter.out.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisherAdapter {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String topic, String key, String payload, String sourceSystem) {
        log.info("🚀 [KAFKA-PUBLISH] Sending event to topic: {} with key: {} from source: {}", 
            topic, key, sourceSystem);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
        if (sourceSystem != null) {
            record.headers().add("source_system", sourceSystem.getBytes(StandardCharsets.UTF_8));
        }

        kafkaTemplate.send(record)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ [KAFKA-SUCCESS] Sent event to topic: {} [offset: {}]", 
                        topic, result.getRecordMetadata().offset());
                } else {
                    log.error("❌ [KAFKA-ERROR] Failed to send event to topic: {}. Error: {}", 
                        topic, ex.getMessage());
                }
            });
    }
}
