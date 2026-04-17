package com.hyperbrain.sopfc.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.application.usecase.SyncEngineService;
import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.infrastructure.adapter.common.dto.AppleReminderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/sync/apple")
@RequiredArgsConstructor
public class AppleWebhookController {

    private final SyncEngineService syncEngineService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveAppleUpdate(@RequestBody AppleReminderDto payload) {
        log.info("📩 [WEBHOOK-IN] Received Apple event: [Source: {}, Type: {}, ID: {}, Title: '{}']", 
            payload.getSource(), payload.getChangeType(), payload.getId(), payload.getTitle());
        
        if ("DELETE".equals(payload.getChangeType())) {
            log.info("🗑️ [WEBHOOK-IN] Deletion requested via Apple webhook for ID: {}", payload.getId());
            // TODO: Implement processExternalDelete in SyncEngine if needed
            return ResponseEntity.accepted().build();
        }

        ExecutableStatus status = Boolean.TRUE.equals(payload.getIsCompleted()) 
            ? ExecutableStatus.DONE : ExecutableStatus.IN_PROGRESS;

        CoreExecutable updatedData = CoreExecutable.builder()
                .name(payload.getTitle())
                .status(status)
                .startTime(payload.getStartDate() != null ? payload.getStartDate() : payload.getTimestamp())
                .endTime(payload.getEndDate())
                .build();
        
        String systemIdentifier = "APPLE_" + payload.getSource().toUpperCase();
        log.debug("⚙️ [WEBHOOK-IN] Mapping payload to domain. Status: {}, System: {}", status, systemIdentifier);
        
        syncEngineService.processExternalUpdate(systemIdentifier, payload.getId(), updatedData);
        
        return ResponseEntity.accepted().build();
    }
}
