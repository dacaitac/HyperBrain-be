package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.sync.application.usecase.SyncEngineService;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/sync/apple")
@RequiredArgsConstructor
public class AppleWebhookController {

    private final SyncEngineService syncEngineService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleAppleReminderChange(
            @RequestBody AppleReminderDto reminder) {
        log.info("🍎 [APPLE-WEBHOOK] Received change notification ({}): '{}' (ID: {})", 
                reminder.getChangeType(), reminder.getTitle(), reminder.getId());

        String type = reminder.getChangeType() != null ? reminder.getChangeType().toUpperCase() : "";

        if ("DELETE".equals(type) || "DELETED".equals(type)) {
            syncEngineService.processExternalDelete("APPLE_REMINDERS", reminder.getId());
            return ResponseEntity.accepted().build();
        }

        CoreExecutable domain = CoreExecutable.builder()
                .name(reminder.getTitle())
                .description(reminder.getNotes())
                .status(Boolean.TRUE.equals(reminder.getIsCompleted()) ? ExecutableStatus.DONE : ExecutableStatus.PENDING)
                .startTime(reminder.getDueDate())
                .applePriority(reminder.getPriority())
                .externalUrl(reminder.getUrl())
                .build();

        if ("CREATE".equals(type) || "CREATED".equals(type) || "UPDATE".equals(type) || "UPDATED".equals(type)) {
            syncEngineService.processExternalUpdate("APPLE_REMINDERS", reminder.getId(), domain);
        } else {
            log.warn("⚠️ [APPLE-WEBHOOK] Unknown change type: {}", type);
        }

        return ResponseEntity.accepted().build();
    }
}
