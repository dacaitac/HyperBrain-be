package com.hyperbrain.sopfc.sync.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.common.infrastructure.config.SyncContextHolder;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.sync.application.util.SyncUtils;
import com.hyperbrain.sopfc.sync.domain.port.in.SyncResourceUseCase;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncResourceRepository;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.NotionSyncAdapter;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto.NotionPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncResourceService implements SyncResourceUseCase {

    private final SyncResourceRepository repository;
    private final SyncEngineService syncEngineService;
    private final NotionSyncAdapter notionSyncAdapter;
    private final ObjectMapper objectMapper;

    @Override
    public void create(Map<String, Object> data) {
        log.info("📥 [RESOURCE] Request received: CREATE");
        repository.save("create", data);
        processIfNotion(data);
    }

    @Override
    public void update(Map<String, Object> data) {
        log.info("📥 [RESOURCE] Request received: UPDATE");
        repository.save("update", data);
        processIfNotion(data);
    }

    @Override
    public void delete(Map<String, Object> data) {
        log.info("📥 [RESOURCE] Request received: DELETE");
        repository.save("delete", data);
    }

    private void processIfNotion(Map<String, Object> payload) {
        try {
            // Notion Automations send data wrapped in a "data" object
            Object dataObj = payload.get("data");
            if (dataObj instanceof Map) {
                NotionPageResponse response = objectMapper.convertValue(dataObj, NotionPageResponse.class);
                if (response != null && response.getId() != null) {
                    String externalId = SyncUtils.normalizeNotionId(response.getId());
                    log.info("🔗 [RESOURCE-BRIDGE] Notion content detected. ID: {}", externalId);

                    CoreExecutable updatedData = notionSyncAdapter.toDomain(response);
                    
                    // Disparamos la actualización forzando el origen NOTION
                    SyncContextHolder.setSource("NOTION");
                    syncEngineService.processExternalUpdate("NOTION", externalId, updatedData);
                    SyncContextHolder.clear();
                    
                    log.info("✅ [RESOURCE-BRIDGE] Sync propagation successful for page: {}", externalId);
                }
            } else {
                log.debug("ℹ️ [RESOURCE-BRIDGE] Payload is not a standard Notion automation object. Skipping bridge.");
            }
        } catch (Exception e) {
            log.error("❌ [RESOURCE-BRIDGE] Critical failure during bridge: {}", e.getMessage(), e);
            SyncContextHolder.clear();
        }
    }
}
