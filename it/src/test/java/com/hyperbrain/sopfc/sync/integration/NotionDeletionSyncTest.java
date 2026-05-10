package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.SopfcApplication;
import com.hyperbrain.sopfc.common.application.outbox.OutboxScheduler;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.it.util.ServiceAssumptions;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.repository.SpringDataSyncMappingRepository;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import org.awaitility.Awaitility;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SopfcApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NotionDeletionSyncTest {

    private static final Logger log = LoggerFactory.getLogger(NotionDeletionSyncTest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private SpringDataCoreExecutableRepository executableRepository;
    @Autowired private SpringDataSyncMappingRepository mappingRepository;
    @Autowired private OutboxScheduler outboxScheduler;
    @Autowired private ObjectMapper objectMapper;

    private static final String APPLE_SERVICE_URL = "http://127.0.0.1:1995";
    private WebClient appleClient;
    private UUID localId;
    private String sharedAppleId;
    private String notionId;

    @BeforeEach
    void setup() {
        appleClient = WebClient.builder().baseUrl(APPLE_SERVICE_URL).build();
        ServiceAssumptions.assumeAppleServicesAreActive();
        
        executableRepository.deleteAll();
        mappingRepository.deleteAll();
        
        localId = UUID.randomUUID();
        notionId = UUID.randomUUID().toString().replace("-", "");
        
        // 1. Crear elemento local
        CoreExecutableEntity entity = CoreExecutableEntity.builder()
                .id(localId)
                .name("Notion-Delete-Test-" + System.currentTimeMillis())
                .status(com.hyperbrain.sopfc.core.domain.model.ExecutableStatus.PENDING)
                .type(com.hyperbrain.sopfc.core.domain.model.CoreExecutable.ExecutableType.TASK)
                .build();
        executableRepository.saveAndFlush(entity);

        // 2. Crear un recordatorio REAL en Apple para poder borrarlo
        Map<String, Object> appleReq = Map.of(
                "title", entity.getName(),
                "calendarName", "Reminders"
        );
        
        var response = appleClient.post().uri("/reminders")
                .bodyValue(appleReq)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        sharedAppleId = (String) response.get("id");
        log.info("🚀 [TEST] Created test reminder in Apple: {}", sharedAppleId);

        // 3. Crear mapeos manuales
        mappingRepository.saveAndFlush(com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.entity.SyncMappingEntity.builder()
                .id(UUID.randomUUID())
                .executableId(localId)
                .externalId(notionId)
                .externalSystem("NOTION")
                .syncStatus("IN_SYNC")
                .build());

        mappingRepository.saveAndFlush(com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.entity.SyncMappingEntity.builder()
                .id(UUID.randomUUID())
                .executableId(localId)
                .externalId(sharedAppleId)
                .externalSystem("APPLE_REMINDERS")
                .syncStatus("IN_SYNC")
                .build());
    }

    @Test
    @DisplayName("Should propagate deletion from Notion to Apple via Webhook")
    void testNotionDeletionPropagatesToApple() throws Exception {
        // Simular Webhook de Notion con archived: true
        Map<String, Object> notionPayload = Map.of(
            "type", "page_updated",
            "data", Map.of(
                "id", notionId,
                "archived", true,
                "properties", Map.of()
            )
        );

        log.info("📢 [TEST] Sending deletion webhook for Notion ID: {}", notionId);
        mockMvc.perform(post("/api/v1/sync/notion/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(notionPayload)))
                .andExpect(status().isAccepted());

        // Procesar outbox
        outboxScheduler.processOutbox();

        // 4. Verificar que en Apple ya no exista (debería dar 404)
        await("Apple Deletion Propagation").atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    try {
                        appleClient.get().uri("/items/{id}", sharedAppleId).retrieve().toBodilessEntity().block();
                        return false; // Sigue existiendo
                    } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
                        return true; // Borrado exitoso
                    }
                });

        log.info("✅ [TEST] Notion deletion successfully propagated to Apple Reminders.");
    }
}

