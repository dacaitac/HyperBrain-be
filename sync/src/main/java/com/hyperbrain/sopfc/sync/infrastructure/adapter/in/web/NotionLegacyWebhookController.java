package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.common.infrastructure.config.SyncContextHolder;
import com.hyperbrain.sopfc.sync.application.usecase.SyncEngineService;
import com.hyperbrain.sopfc.sync.application.util.SyncUtils;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.NotionSyncAdapter;
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
    private final NotionSyncAdapter notionSyncAdapter;

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

            // Prioritize deletion/archival
            if ("page.deleted".equalsIgnoreCase(payload.getType())) {
                log.info("🗑️ [NOTION-LEGACY-WEBHOOK] Page DELETED event. Triggering sync for: {}", externalId);
                syncEngineService.processExternalDelete("NOTION", externalId);
            } else if ("page.properties_updated".equalsIgnoreCase(payload.getType()) || "page.created".equalsIgnoreCase(payload.getType())) {
                // For updates or creations via automations, we verify if the page is currently archived.
                // This is crucial because a user might have archived it and the automation triggers an 'update' instead of 'delete'.
                log.info("🔍 [NOTION-LEGACY-WEBHOOK] Verifying status for event {} on: {}", payload.getType(), externalId);
                if (notionSyncAdapter.isArchived(externalId)) {
                    log.info("🗑️ [NOTION-LEGACY-WEBHOOK] Page is ARCHIVED. Triggering delete for: {}", externalId);
                    syncEngineService.processExternalDelete("NOTION", externalId);
                } else {
                    log.debug("⏭️ [NOTION-LEGACY-WEBHOOK] Page is active. Standard sync will handle it if needed.");
                    // Optional: we could trigger a fetch and update here if we want even faster updates
                    notionSyncAdapter.fetchById(externalId).ifPresent(result -> 
                        syncEngineService.processExternalUpdate("NOTION", externalId, result.executable())
                    );
                }
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
