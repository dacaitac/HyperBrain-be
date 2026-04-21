package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.apple;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleGenericResponse;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderCreateRequest;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AppleSentinelSyncAdapter implements ExternalSyncPort {

    private final WebClient webClient;

    public AppleSentinelSyncAdapter(@Value("${apple.sentinel.url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public List<ExternalSyncResult> fetchDelta() {
        return Collections.emptyList();
    }

    @Override
    public Optional<ExternalSyncResult> fetchById(String externalId) {
        return Optional.empty();
    }

    @Override
    public void pushUpdate(CoreExecutable executable, String externalId) {
        log.info("📤 [APPLE-REMINDERS] Pushing update: '{}' (ID: {})", executable.getName(), externalId);
        AppleReminderUpdateRequest dto = AppleReminderUpdateRequest.builder()
                .id(externalId)
                .newTitle(executable.getName())
                .newNotes(executable.getDescription())
                .newDueDate(executable.getStartTime())
                .isCompleted(executable.getStatus() == ExecutableStatus.DONE)
                .build();

        webClient.put()

                .uri("/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("✅ [APPLE-REMINDERS] Update successful for {}", externalId))
                .doOnError(e -> log.error("❌ [APPLE-REMINDERS] Update failed for {}: {}", externalId, e.getMessage()))
                .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                .subscribe();
    }

    @Override
    public String pushCreate(CoreExecutable executable) {
        log.info("➕ [APPLE-REMINDERS] Pushing creation: '{}'", executable.getName());
        AppleReminderCreateRequest dto = AppleReminderCreateRequest.builder()
                .title(executable.getName())
                .notes(executable.getDescription())
                .isCompleted(executable.getStatus() == ExecutableStatus.DONE)
                .dueDate(executable.getStartTime())
                .calendarName("Reminders")
                .build();

        log.info("📤 [APPLE-REMINDERS] Create Request: {}", dto);

        AppleGenericResponse response = webClient.post()
                .uri("/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(AppleGenericResponse.class)
                .block();

        return response != null ? response.getId() : null;
    }

    @Override
    public void pushDelete(String externalId) {
        log.info("🗑️ [APPLE-REMINDERS] Pushing deletion: {}", externalId);
        webClient.delete()
                .uri("/items/{id}", externalId)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("✅ [APPLE-REMINDERS] Deletion successful: {}", externalId))
                .doOnError(e -> log.error("❌ [APPLE-REMINDERS] Deletion failed: {}", e.getMessage()))
                .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                .subscribe();
    }

    @Override
    public String getSystemIdentifier() {
        return "APPLE_REMINDERS";
    }
}
