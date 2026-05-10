package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleGenericResponse;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderCreateRequest;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderUpdateRequest;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderDto;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleEventCreateRequest;
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

    private String formatAppleDate(java.time.OffsetDateTime dt) {
        if (dt == null) return null;
        // Si es medianoche exacta, asumimos que es una tarea de "todo el día" o solo fecha
        if (dt.getHour() == 0 && dt.getMinute() == 0 && dt.getSecond() == 0) {
            return dt.toLocalDate().toString(); // YYYY-MM-DD
        }
        return dt.format(ISO_FORMATTER);
    }

    @Override
    public void pushUpdate(CoreExecutable executable, String externalId) {
        boolean isDone = executable.getStatus() == ExecutableStatus.DONE;
        log.info("📤 [APPLE-PUSH] Updating: '{}' (ID: {}). Status: {}, isCompleted: {}", 
                executable.getName(), externalId, executable.getStatus(), isDone);

        // La API de Apple usa /reminders para actualizaciones de ambos tipos
        AppleReminderUpdateRequest dto = AppleReminderUpdateRequest.builder()
                .id(externalId)
                .newTitle(executable.getName())
                .newNotes(executable.getDescription())
                .isoStartDate(formatAppleDate(executable.getStartTime()))
                .newEndDate(formatAppleDate(executable.getEndTime()))
                .isCompleted(isDone)
                .priority(executable.getApplePriority())
                .alarms(executable.getAlarms() != null ? executable.getAlarms().stream().map(this::formatAppleDate).toList() : null)
                .recurrence(executable.getRecurrence())
                .url(executable.getExternalUrl())
                .build();

        webClient.put()
                .uri("/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(res -> log.info("✅ [APPLE-PUSH] Update successfully sent."))
                .doOnError(e -> log.error("❌ [APPLE-PUSH] Failed to send update: {}", e.getMessage()))
                .block();
    }

    @Override
    public String pushCreate(CoreExecutable executable) {
        boolean isEvent = executable.getType() == CoreExecutable.ExecutableType.ACTIVITY || 
                         executable.getType() == CoreExecutable.ExecutableType.AGENDA;
        
        String endpoint = isEvent ? "/events" : "/reminders";
        log.info("➕ [APPLE-PUSH] Creating {} in Apple: '{}'", isEvent ? "Event" : "Reminder", executable.getName());

        Object dto;
        if (isEvent) {
            dto = AppleEventCreateRequest.builder()
                    .title(executable.getName())
                    .notes(executable.getDescription())
                    .startDate(executable.getStartTime() != null ? executable.getStartTime() : java.time.OffsetDateTime.now())
                    .endDate(executable.getEndTime())
                    .calendarName(executable.getSourceCalendar())
                    .build();
        } else {
            dto = AppleReminderCreateRequest.builder()
                    .title(executable.getName())
                    .notes(executable.getDescription())
                    .isCompleted(executable.getStatus() == ExecutableStatus.DONE)
                    .date(formatAppleDate(executable.getStartTime()))
                    .priority(executable.getApplePriority())
                    .alarms(executable.getAlarms() != null ? executable.getAlarms().stream().map(this::formatAppleDate).toList() : null)
                    .recurrence(executable.getRecurrence())
                    .url(executable.getExternalUrl())
                    .calendarName(executable.getSourceCalendar())
                    .build();
        }

        AppleGenericResponse response = webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
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
                .uri("/reminders") // La API usa el mismo endpoint de borrado para ambos
                .bodyValue(java.util.Map.of("id", externalId))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(res -> log.info("✅ [APPLE-PUSH] Delete success."))
                .block();
    }

    @Override
    public String getSystemIdentifier() {
        return "APPLE_SENTINEL"; // Identificador genérico para el adaptador que maneja ambos vía Sentinel
    }

    private CoreExecutable toDomain(AppleReminderDto dto) {
        return CoreExecutable.builder()
                .name(dto.getTitle())
                .description(dto.getNotes())
                .status(Boolean.TRUE.equals(dto.getIsCompleted()) ? ExecutableStatus.DONE : ExecutableStatus.PENDING)
                .startTime(dto.getDueDate())
                .sourceCalendar(dto.getCalendarName())
                .applePriority(dto.getPriority())
                .alarms(dto.getAlarms())
                .recurrence(dto.getRecurrence())
                .externalUrl(dto.getUrl())
                .completionDate(dto.getCompletionDate())
                .lastModifiedDate(dto.getLastModifiedDate())
                .build();
    }
}
