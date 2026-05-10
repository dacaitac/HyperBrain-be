package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.common.infrastructure.config.SyncContextHolder;
import com.hyperbrain.sopfc.sync.application.usecase.SyncEngineService;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.sync.application.util.SyncUtils;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.NotionSyncAdapter;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto.NotionWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/sync/notion")
@RequiredArgsConstructor
public class NotionWebhookController {

    private final SyncEngineService syncEngineService;
    private final NotionSyncAdapter notionSyncAdapter;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleNotionWebhook(@RequestBody NotionWebhookPayload payload) {
        log.info("📥 [NOTION-WEBHOOK] Received payload: {}", payload);
        
        try {
            if (payload == null || payload.getData() == null || payload.getData().getId() == null) {
                log.warn("⚠️ [NOTION-WEBHOOK] Empty or invalid payload received.");
                return ResponseEntity.accepted().build();
            }

            // Normalizamos el ID inmediatamente al recibirlo
            String externalId = SyncUtils.normalizeNotionId(payload.getData().getId());
            log.info("🔍 [NOTION-WEBHOOK] Normalized ID: {}. Archived: {}", externalId, payload.getData().isArchived());

            SyncContextHolder.setSource("NOTION");

            // Prioritize deletion/archival by checking both the payload flag and the event type
            boolean isDeletionEvent = "page.deleted".equalsIgnoreCase(payload.getType());
            
            if (payload.getData().isArchived() || isDeletionEvent) {
                log.info("🗑️ [NOTION-WEBHOOK] Page is ARCHIVED or DELETED (type: {}). Triggering external delete for: {}", 
                    payload.getType(), externalId);
                syncEngineService.processExternalDelete("NOTION", externalId);
            } else {
                // Proactive check: if we receive an update, we verify if it might have been archived recently 
                // but the payload hasn't reflected it yet (standard webhooks can be slightly out of sync).
                if (notionSyncAdapter.isArchived(externalId)) {
                    log.info("🗑️ [NOTION-WEBHOOK] Proactive check revealed page is ARCHIVED. Triggering delete for: {}", externalId);
                    syncEngineService.processExternalDelete("NOTION", externalId);
                } else {
                    CoreExecutable updatedData = notionSyncAdapter.toDomain(payload.getData());
                    syncEngineService.processExternalUpdate("NOTION", externalId, updatedData);
                    log.info("✅ [NOTION-WEBHOOK] Processing update for page: {}", externalId);
                }
            }
        } catch (Exception e) {
            log.error("❌ [NOTION-WEBHOOK] Error processing webhook: {}", e.getMessage(), e);
        } finally {
            SyncContextHolder.clear();
        }

        return ResponseEntity.accepted().build();
    }
}
