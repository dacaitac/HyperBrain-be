package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.SopfcApplication;
import com.hyperbrain.sopfc.common.application.outbox.OutboxScheduler;
import com.hyperbrain.sopfc.common.infrastructure.persistence.entity.OutboxEventEntity;
import com.hyperbrain.sopfc.common.infrastructure.persistence.repository.OutboxEventRepository;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.it.util.ServiceAssumptions;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.NotionSyncAdapter;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.repository.SpringDataSyncMappingRepository;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import org.awaitility.Awaitility;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SopfcApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ComprehensiveExternalSyncTest {

    private static final Logger log = LoggerFactory.getLogger(ComprehensiveExternalSyncTest.class);

    @Autowired private SpringDataCoreExecutableRepository executableRepository;
    @Autowired private OutboxEventRepository outboxRepository;
    @Autowired private OutboxScheduler outboxScheduler;
    @Autowired private SpringDataSyncMappingRepository mappingRepository;
    @Autowired private NotionSyncAdapter notionAdapter;
    @Autowired private ObjectMapper objectMapper;

    private static final String APPLE_SERVICE_URL = "http://127.0.0.1:1995";
    private WebClient appleClient;
    private UUID localId;
    private String sharedAppleId;
    private String sharedNotionId;
    private String testTitle;

    @BeforeAll
    void init() {
        appleClient = WebClient.builder().baseUrl(APPLE_SERVICE_URL).build();
        ServiceAssumptions.assumeAppleServicesAreActive();
        mappingRepository.deleteAll();
        executableRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("1. Creación en BD Local -> Sincronización con Apple y Notion")
    void step1_CreateLocalAndSync() throws Exception {
        localId = UUID.randomUUID();
        testTitle = "Comp-Test-" + System.currentTimeMillis();
        
        log.info("🚀 [TEST] Creando ejecutable local: {}", testTitle);
        CoreExecutableEntity entity = CoreExecutableEntity.builder()
                .id(localId)
                .name(testTitle)
                .status(ExecutableStatus.PENDING)
                .type(com.hyperbrain.sopfc.core.domain.model.CoreExecutable.ExecutableType.TASK)
                .build();
        executableRepository.saveAndFlush(entity);

        // Disparamos evento vía Outbox
        String payload = objectMapper.writeValueAsString(Map.of(
                "newStatus", "PENDING",
                "sourceSystem", "INTERNAL"
        ));
        
        OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                .aggregateType("CORE_EXECUTABLE")
                .aggregateId(localId.toString())
                .eventType("EXECUTABLE_CREATED")
                .payload(payload)
                .sourceSystem("INTERNAL")
                .build();
        outboxRepository.saveAndFlush(outboxEvent);

        log.info("📢 [TEST] Procesando Outbox para creación...");
        outboxScheduler.processOutbox();

        // Validar Apple
        await("Apple Mapping").atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> mappingRepository.findByExecutableIdAndExternalSystem(localId, "APPLE_REMINDERS").isPresent());
        
        sharedAppleId = mappingRepository.findByExecutableIdAndExternalSystem(localId, "APPLE_REMINDERS").get().getExternalId();
        
        JsonNode appleItem = appleClient.get().uri("/items/{id}", sharedAppleId).retrieve().bodyToMono(JsonNode.class).block();
        assertNotNull(appleItem);
        assertEquals(testTitle, appleItem.get("title").asText());
        log.info("✅ [TEST] Verificado en Apple: {}", sharedAppleId);

        // Validar Notion
        await("Notion Mapping").atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> mappingRepository.findByExecutableIdAndExternalSystem(localId, "NOTION").isPresent());
        
        sharedNotionId = mappingRepository.findByExecutableIdAndExternalSystem(localId, "NOTION").get().getExternalId();
        
        var notionResult = notionAdapter.fetchById(sharedNotionId);
        assertTrue(notionResult.isPresent());
        assertEquals(testTitle, notionResult.get().executable().getName());
        log.info("✅ [TEST] Verificado en Notion: {}", sharedNotionId);
    }

    @Test
    @Order(2)
    @DisplayName("2. Cambio de Fecha -> Propagación")
    void step2_UpdateDate() throws Exception {
        OffsetDateTime newDate = OffsetDateTime.now().plusDays(5).withNano(0);
        log.info("📅 [TEST] Actualizando fecha a: {}", newDate);
        
        CoreExecutableEntity entity = executableRepository.findById(localId).get();
        entity.setStartTime(newDate);
        entity.setPlanned(true);
        executableRepository.saveAndFlush(entity);

        triggerUpdateEvent("UPDATE_DATE");

        // Validar Apple
        await("Apple Date Update").atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    JsonNode appleItem = appleClient.get().uri("/items/{id}", sharedAppleId).retrieve().bodyToMono(JsonNode.class).block();
                    return appleItem != null && appleItem.has("date") && !appleItem.get("date").isNull();
                });
        log.info("✅ [TEST] Fecha actualizada en Apple.");

        // Validar Notion
        await("Notion Date Update").atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    var notionItem = notionAdapter.fetchById(sharedNotionId).get();
                    return notionItem.executable().getStartTime() != null;
                });
        log.info("✅ [TEST] Fecha actualizada en Notion.");
    }

    @Test
    @Order(3)
    @DisplayName("3. Cambio a Completado -> Propagación")
    void step3_UpdateStatus() throws Exception {
        log.info("🏁 [TEST] Marcando como COMPLETADO");
        CoreExecutableEntity entity = executableRepository.findById(localId).get();
        entity.setStatus(ExecutableStatus.DONE);
        executableRepository.saveAndFlush(entity);

        triggerUpdateEvent("DONE");

        // Validar Apple
        await("Apple Status Update").atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    JsonNode appleItem = appleClient.get().uri("/items/{id}", sharedAppleId).retrieve().bodyToMono(JsonNode.class).block();
                    return appleItem != null && appleItem.get("isCompleted").asBoolean();
                });
        log.info("✅ [TEST] Status DONE en Apple.");

        // Validar Notion
        await("Notion Status Update").atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    var notionItem = notionAdapter.fetchById(sharedNotionId).get();
                    return notionItem.executable().getStatus() == ExecutableStatus.DONE;
                });
        log.info("✅ [TEST] Status DONE en Notion.");
    }

    @Test
    @Order(4)
    @DisplayName("4. Cambio de Nombre -> Propagación")
    void step4_UpdateName() throws Exception {
        String newName = "Updated-Name-" + System.currentTimeMillis();
        log.info("📝 [TEST] Cambiando nombre a: {}", newName);
        
        CoreExecutableEntity entity = executableRepository.findById(localId).get();
        entity.setName(newName);
        executableRepository.saveAndFlush(entity);

        triggerUpdateEvent("UPDATE_NAME");

        // Validar Apple
        await("Apple Name Update").atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    JsonNode appleItem = appleClient.get().uri("/items/{id}", sharedAppleId).retrieve().bodyToMono(JsonNode.class).block();
                    return appleItem != null && newName.equals(appleItem.get("title").asText());
                });
        log.info("✅ [TEST] Nombre actualizado en Apple.");

        // Validar Notion
        await("Notion Name Update").atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    var notionItem = notionAdapter.fetchById(sharedNotionId).get();
                    return notionItem != null && newName.equals(notionItem.executable().getName());
                });
        log.info("✅ [TEST] Nombre actualizado en Notion.");
    }

    @AfterAll
    void cleanup() {
        log.info("🧹 [TEST] Limpiando recursos externos...");
        try {
            if (sharedAppleId != null) {
                appleClient.method(org.springframework.http.HttpMethod.DELETE).uri("/reminders")
                        .bodyValue(Map.of("id", sharedAppleId)).retrieve().toBodilessEntity().block();
            }
        } catch (Exception e) {
            log.warn("No se pudo limpiar Apple: {}", e.getMessage());
        }
        try {
            if (sharedNotionId != null) {
                notionAdapter.pushDelete(sharedNotionId);
            }
        } catch (Exception e) {
            log.warn("No se pudo limpiar Notion: {}", e.getMessage());
        }
    }

    private void triggerUpdateEvent(String newStatus) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "newStatus", newStatus,
                "sourceSystem", "INTERNAL"
        ));
        OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                .aggregateType("CORE_EXECUTABLE")
                .aggregateId(localId.toString())
                .eventType("EXECUTABLE_STATUS_CHANGED")
                .payload(payload)
                .sourceSystem("INTERNAL")
                .build();
        outboxRepository.saveAndFlush(outboxEvent);
        outboxScheduler.processOutbox();
    }
}

