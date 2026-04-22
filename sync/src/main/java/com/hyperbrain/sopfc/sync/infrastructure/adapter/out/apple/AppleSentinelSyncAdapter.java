package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleGenericResponse;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderCreateRequest;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderUpdateRequest;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AppleSentinelSyncAdapter implements ExternalSyncPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public AppleSentinelSyncAdapter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, @Value("${apple.sentinel.url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                })
                .build();
    }

    @Override
    public List<ExternalSyncResult> fetchDelta() {
        return Collections.emptyList();
    }

    @Override
    public Optional<ExternalSyncResult> fetchById(String externalId) {
        log.info("🔍 [APPLE-FETCH] Fetching reminder: {}", externalId);
        return webClient.get()
                .uri("/items/{id}", externalId)
                .retrieve()
                .bodyToMono(AppleReminderDto.class)
                .doOnNext(dto -> log.debug("✅ [APPLE-FETCH] Received: {}", dto))
                .map(dto -> new ExternalSyncResult(toDomain(dto), dto.getId()))
                .blockOptional();
    }

    @Override
    public void pushUpdate(CoreExecutable executable, String externalId) {
        boolean isDone = executable.getStatus() == ExecutableStatus.DONE;
        log.info("📤 [APPLE-PUSH] Updating: '{}' (ID: {}). Status: {}, isCompleted: {}", 
                executable.getName(), externalId, executable.getStatus(), isDone);

        AppleReminderUpdateRequest dto = AppleReminderUpdateRequest.builder()
                .id(externalId)
                .newTitle(executable.getName())
                .newNotes(executable.getDescription())
                .isoStartDate(executable.getStartTime() != null ? executable.getStartTime().format(ISO_FORMATTER) : null)
                .newEndDate(executable.getEndTime() != null ? executable.getEndTime().format(ISO_FORMATTER) : null)
                .isCompleted(isDone)
                .priority(executable.getApplePriority())
                .url(executable.getExternalUrl())
                .build();

        String updatePayload;
        try {
            updatePayload = objectMapper.writeValueAsString(dto);
            log.debug("📤 [APPLE-PUSH] Payload: {}", updatePayload);
        } catch (Exception e) {
            log.error("❌ [APPLE-PUSH] Error serializing payload: {}", e.getMessage());
            return;
        }

        webClient.put()
                .uri("/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatePayload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(res -> log.info("✅ [APPLE-PUSH] Update successfully sent. Response: {}", res))
                .doOnError(e -> log.error("❌ [APPLE-PUSH] Failed to send update: {}", e.getMessage()))
                .block();
    }

    @Override
    public String pushCreate(CoreExecutable executable) {
        log.info("➕ [APPLE-PUSH] Creating: '{}'", executable.getName());
        AppleReminderCreateRequest dto = AppleReminderCreateRequest.builder()
                .title(executable.getName())
                .notes(executable.getDescription())
                .isCompleted(executable.getStatus() == ExecutableStatus.DONE)
                .date(executable.getStartTime() != null ? executable.getStartTime().format(ISO_FORMATTER) : null)
                .priority(executable.getApplePriority())
                .url(executable.getExternalUrl())
                .calendarName("Reminders")
                .build();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(dto);
            log.debug("➕ [APPLE-PUSH] Payload: {}", payload);
        } catch (Exception e) {
            log.error("❌ [APPLE-PUSH] Error serializing create payload: {}", e.getMessage());
            return null;
        }

        AppleGenericResponse response = webClient.post()
                .uri("/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(AppleGenericResponse.class)
                .doOnSuccess(res -> log.info("✅ [APPLE-PUSH] Creation success. ID: {}", res.getId()))
                .block();

        return response != null ? response.getId() : null;
    }

    @Override
    public void pushDelete(String externalId) {
        log.info("🗑️ [APPLE-PUSH] Deleting: {}", externalId);
        webClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/reminders")
                .bodyValue(java.util.Map.of("id", externalId))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(res -> log.info("✅ [APPLE-PUSH] Delete success. Response: {}", res))
                .block();
    }

    @Override
    public String getSystemIdentifier() {
        return "APPLE_REMINDERS";
    }

    private CoreExecutable toDomain(AppleReminderDto dto) {
        return CoreExecutable.builder()
                .name(dto.getTitle())
                .description(dto.getNotes())
                .status(Boolean.TRUE.equals(dto.getIsCompleted()) ? ExecutableStatus.DONE : ExecutableStatus.PENDING)
                .startTime(dto.getDueDate())
                .applePriority(dto.getPriority())
                .externalUrl(dto.getUrl())
                .completionDate(dto.getCompletionDate())
                .lastModifiedDate(dto.getLastModifiedDate())
                .build();
    }
}
