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
    public ResponseEntity<Void> handleAppleChange(
            @RequestBody AppleReminderDto item) {
        
        String sourceSystem = "APPLE_SENTINEL";
        
        // Default calendar name logic: calendarName -> source -> "Reminders"
        String calendarName = item.getCalendarName();
        if (calendarName == null) {
            calendarName = item.getSource() != null ? item.getSource() : "Reminders";
        }
        
        log.info("🍎 [APPLE-WEBHOOK] Received change notification ({}): '{}' (ID: {}) in '{}'", 
                item.getChangeType(), item.getTitle(), item.getId(), calendarName);

        String type = item.getChangeType() != null ? item.getChangeType().toUpperCase() : "";

        if ("DELETE".equals(type) || "DELETED".equals(type)) {
            syncEngineService.processExternalDelete(sourceSystem, item.getId());
            return ResponseEntity.accepted().build();
        }

        CoreExecutable.ExecutableType domainType = CoreExecutable.ExecutableType.TASK;
        if ("Events".equalsIgnoreCase(item.getSource()) || "Event".equalsIgnoreCase(item.getSource())) {
            domainType = CoreExecutable.ExecutableType.ACTIVITY;
        }

        CoreExecutable domain = CoreExecutable.builder()
                .name(item.getTitle())
                .description(item.getNotes())
                .type(domainType)
                .status(Boolean.TRUE.equals(item.getIsCompleted()) ? ExecutableStatus.DONE : ExecutableStatus.PENDING)
                .startTime(item.getDueDate())
                .sourceCalendar(calendarName)
                .applePriority(item.getPriority())
                .alarms(item.getAlarms())
                .recurrence(item.getRecurrence())
                .externalUrl(item.getUrl())
                .build();

        if ("CREATE".equals(type) || "CREATED".equals(type) || "UPDATE".equals(type) || "UPDATED".equals(type)) {
            syncEngineService.processExternalUpdate(sourceSystem, item.getId(), domain);
        } else {
            log.warn("⚠️ [APPLE-WEBHOOK] Unknown change type: {}", type);
        }

        return ResponseEntity.accepted().build();
    }
}
