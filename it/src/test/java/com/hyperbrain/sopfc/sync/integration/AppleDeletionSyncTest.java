package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.SopfcApplication;
import com.hyperbrain.sopfc.common.application.outbox.OutboxScheduler;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderDto;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.NotionSyncAdapter;
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

import org.awaitility.Awaitility;
import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SopfcApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AppleDeletionSyncTest {

    private static final Logger log = LoggerFactory.getLogger(AppleDeletionSyncTest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private SpringDataCoreExecutableRepository executableRepository;
    @Autowired private SpringDataSyncMappingRepository mappingRepository;
    @Autowired private OutboxScheduler outboxScheduler;
    @Autowired private NotionSyncAdapter notionAdapter;
    @Autowired private ObjectMapper objectMapper;

    private UUID localId;
    private String sharedNotionId;

    @BeforeEach
    void setup() {
        executableRepository.deleteAll();
        mappingRepository.deleteAll();
        
        // 1. Crear elemento local y linkearlo a Notion para poder borrarlo luego
        localId = UUID.randomUUID();
        CoreExecutableEntity entity = CoreExecutableEntity.builder()
                .id(localId)
                .name("Delete-Test-" + System.currentTimeMillis())
                .status(com.hyperbrain.sopfc.core.domain.model.ExecutableStatus.PENDING)
                .type(com.hyperbrain.sopfc.core.domain.model.CoreExecutable.ExecutableType.TASK)
                .build();
        executableRepository.saveAndFlush(entity);

        // Forzar creación en Notion para tener un ID real que borrar
        sharedNotionId = notionAdapter.pushCreate(com.hyperbrain.sopfc.core.domain.model.CoreExecutable.builder()
                .name(entity.getName())
                .status(entity.getStatus())
                .type(entity.getType())
                .build());
        
        log.info("🚀 [TEST] Created test page in Notion: {}", sharedNotionId);

        // Crear mapeo manual para simular que ya estaban sincronizados
        com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.entity.SyncMappingEntity mapping = 
            com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.entity.SyncMappingEntity.builder()
                .id(UUID.randomUUID())
                .executableId(localId)
                .externalId(sharedNotionId)
                .externalSystem("NOTION")
                .syncStatus("IN_SYNC")
                .build();
        mappingRepository.saveAndFlush(mapping);
        
        // También simulamos el ID de Apple (aunque no exista realmente en Apple para este test, 
        // lo importante es que el webhook use este ID)
    }

    @Test
    @DisplayName("Should propagate deletion from Apple to Notion via Webhook")
    void testAppleDeletionPropagatesToNotion() throws Exception {
        String appleId = "fake-apple-id-" + UUID.randomUUID();
        
        // Mapeamos el ID de Apple al ejecutable local
        com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.entity.SyncMappingEntity appleMapping = 
            com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.entity.SyncMappingEntity.builder()
                .id(UUID.randomUUID())
                .executableId(localId)
                .externalId(appleId)
                .externalSystem("APPLE_REMINDERS")
                .syncStatus("IN_SYNC")
                .build();
        mappingRepository.saveAndFlush(appleMapping);

        // 2. Simular Webhook de Apple con "deleted" (minúsculas como envía Sentinel)
        AppleReminderDto deletionDto = AppleReminderDto.builder()
                .id(appleId)
                .title("Irrelevant")
                .changeType("deleted")
                .build();

        log.info("📢 [TEST] Sending deletion webhook for Apple ID: {}", appleId);
        mockMvc.perform(post("/api/v1/sync/apple/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deletionDto)))
                .andExpect(status().isAccepted());

        // 3. El controlador debió disparar processExternalDelete -> EXECUTABLE_DELETED event -> Outbox
        // Procesamos outbox
        outboxScheduler.processOutbox();

        // 4. Verificar que en Notion ya no exista (o esté archivada)
        // La implementación actual de NotionSyncAdapter.pushDelete archiva la página
        await("Notion Archival").atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> notionAdapter.isArchived(sharedNotionId));
        log.info("✅ [TEST] Deletion successfully propagated to Notion.");
    }
}

