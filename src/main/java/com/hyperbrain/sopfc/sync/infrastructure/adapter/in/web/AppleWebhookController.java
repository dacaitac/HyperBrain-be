package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.sync.application.usecase.SyncEngineService;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/apple")
@RequiredArgsConstructor
public class AppleWebhookController {

    private final SyncEngineService syncEngineService;

    @PostMapping("/reminders")
    public ResponseEntity<Void> handleAppleReminderChange(@RequestBody com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderDto reminder) {
        log.info("🍎 [APPLE-WEBHOOK] Received change notification ({}): '{}' (ID: {})", 
                reminder.getChangeType(), reminder.getTitle(), reminder.getId());
        
        if ("DELETE".equals(reminder.getChangeType())) {
            syncEngineService.processExternalDelete("APPLE_REMINDERS", reminder.getId());
            return ResponseEntity.accepted().build();
        }

        com.hyperbrain.sopfc.core.domain.model.CoreExecutable domain = com.hyperbrain.sopfc.core.domain.model.CoreExecutable.builder()
                .name(reminder.getTitle())
                .description(reminder.getNotes())
                .status(reminder.getIsCompleted() ? com.hyperbrain.sopfc.core.domain.model.ExecutableStatus.DONE : com.hyperbrain.sopfc.core.domain.model.ExecutableStatus.PENDING)
                .startTime(reminder.getDueDate())
                .build();

        syncEngineService.processExternalUpdate("APPLE_REMINDERS", reminder.getId(), domain);
        
        return ResponseEntity.accepted().build();
    }
}
