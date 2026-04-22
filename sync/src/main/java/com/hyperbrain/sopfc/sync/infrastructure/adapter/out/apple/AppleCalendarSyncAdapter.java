package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.apple;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleEventCreateRequest;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleEventUpdateRequest;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleGenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AppleCalendarSyncAdapter implements ExternalSyncPort {

    private final WebClient webClient;

    public AppleCalendarSyncAdapter(WebClient.Builder webClientBuilder, 
                                   @Value("${apple.sentinel.url}") String sentinelUrl) {
        this.webClient = webClientBuilder
                .baseUrl(sentinelUrl)
                .build();
    }

    @Override
    public Optional<ExternalSyncResult> fetchById(String externalId) {
        return Optional.empty();
    }

    @Override
    public List<ExternalSyncResult> fetchDelta() {
        return Collections.emptyList();
    }

    @Override
    public void pushUpdate(CoreExecutable executable, String externalId) {
        log.info("📤 [APPLE-CALENDAR] Pushing update for event: {}", externalId);
        AppleEventUpdateRequest dto = AppleEventUpdateRequest.builder()
                .id(externalId)
                .newTitle(executable.getName())
                .isCompleted(executable.getStatus() == ExecutableStatus.DONE)
                .newStartDate(executable.getStartTime())
                .newEndDate(executable.getEndTime())
                .build();

        webClient.put()
                .uri("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(AppleGenericResponse.class)
                .doOnSuccess(res -> log.info("✅ [APPLE-CALENDAR] Update successful for {}", externalId))
                .doOnError(e -> log.error("❌ [APPLE-CALENDAR] Update failed for {}: {}", externalId, e.getMessage()))
                .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                .subscribe();
    }

    @Override
    public String pushCreate(CoreExecutable executable) {
        log.info("📤 [APPLE-CALENDAR] Pushing creation: '{}'", executable.getName());
        OffsetDateTime start = executable.getStartTime() != null ? executable.getStartTime() : OffsetDateTime.now();
        
        AppleEventCreateRequest dto = AppleEventCreateRequest.builder()
                .title(executable.getName())
                .startDate(start)
                .endDate(executable.getEndTime())
                .build();

        AppleGenericResponse response = webClient.post()
                .uri("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(AppleGenericResponse.class)
                .doOnSuccess(r -> log.info("✅ [APPLE-CALENDAR] Creation successful. ExtID: {}", r.getId()))
                .block();

        return response != null ? response.getId() : null;
    }

    @Override
    public void pushDelete(String externalId) {
        log.info("📤 [APPLE-CALENDAR] Pushing deletion for event: {}", externalId);
        webClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("id", externalId))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("✅ [APPLE-CALENDAR] Deletion successful for {}", externalId))
                .doOnError(e -> log.error("❌ [APPLE-CALENDAR] Deletion failed: {}", e.getMessage()))
                .subscribe();
    }

    @Override
    public String getSystemIdentifier() {
        return "APPLE_CALENDAR";
    }
}
