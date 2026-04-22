package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.common.infrastructure.config.SyncContextHolder;
import com.hyperbrain.sopfc.sync.application.usecase.SyncEngineService;
import com.hyperbrain.sopfc.sync.application.util.SyncUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to handle Notion Automations / Native Webhooks.
 * Specifically handles 'page.deleted' events that are sent to a different URL
 * than the standard integration webhooks.
 */
@Slf4j
@RestController
@RequestMapping("/api/notion/webhook")
@RequiredArgsConstructor
public class NotionLegacyWebhookController {

    private final SyncEngineService syncEngineService;

    @PostMapping
    public ResponseEntity<Void> handleLegacyWebhook(@RequestBody NotionAutomationPayload payload) {
        log.info("📥 [NOTION-LEGACY-WEBHOOK] Received event: {}", payload.getType());
        
        try {
            if (payload.getEntity() == null || payload.getEntity().getId() == null) {
                log.warn("⚠️ [NOTION-LEGACY-WEBHOOK] Invalid payload: missing entity/id");
                return ResponseEntity.ok().build();
            }

            String externalId = SyncUtils.normalizeNotionId(payload.getEntity().getId());
            SyncContextHolder.setSource("NOTION");

            if ("page.deleted".equalsIgnoreCase(payload.getType())) {
                log.info("🗑️ [NOTION-LEGACY-WEBHOOK] Page DELETED. Triggering sync for: {}", externalId);
                syncEngineService.processExternalDelete("NOTION", externalId);
            } else {
                log.debug("⏭️ [NOTION-LEGACY-WEBHOOK] Ignoring event type: {}", payload.getType());
            }
        } catch (Exception e) {
            log.error("❌ [NOTION-LEGACY-WEBHOOK] Error: {}", e.getMessage());
        } finally {
            SyncContextHolder.clear();
        }

        return ResponseEntity.ok().build();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NotionAutomationPayload {
        private String id;
        private String type; // page.created, page.deleted, page.properties_updated
        private NotionEntity entity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NotionEntity {
        private String id;
        private String object; // "page"
    }
}
