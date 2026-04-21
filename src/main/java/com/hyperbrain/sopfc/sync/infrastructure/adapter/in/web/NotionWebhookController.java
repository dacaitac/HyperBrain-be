package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.sync.application.usecase.SyncEngineService;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
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
        log.info("📥 [NOTION-WEBHOOK] Processing event type: {}", payload.getType());
        
        if (payload.getData() == null || payload.getData().getId() == null) {
            log.warn("⚠️ [NOTION-WEBHOOK] Received payload with missing data or ID. Skipping.");
            return ResponseEntity.badRequest().build();
        }

        String externalId = payload.getData().getId().replace("-", "");
        log.info("🔍 [NOTION-WEBHOOK] Target Page ID: {}", externalId);

        try {
            // Convertimos el DTO de Notion al modelo de dominio usando el adaptador existente
            CoreExecutable updatedData = notionSyncAdapter.toDomain(payload.getData());
            
            // Procesamos la actualización externa
            syncEngineService.processExternalUpdate("NOTION", externalId, updatedData);
            
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("❌ [NOTION-WEBHOOK] Error processing webhook for page {}: {}", externalId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
