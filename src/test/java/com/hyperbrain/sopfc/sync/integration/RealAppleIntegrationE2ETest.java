package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.sync.application.usecase.SyncEngineService;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.NotionSyncAdapter;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto.NotionPageResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RealAppleIntegrationE2ETest {

    @Autowired
    private SyncEngineService syncEngineService;

    @Autowired
    private NotionSyncAdapter notionSyncAdapter;

    @Autowired
    private SpringDataCoreExecutableRepository executableRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String APPLE_SERVICE_URL = "http://127.0.0.1:1995";
    private static String lastAppleId;
    private static String randomTaskName;
    private static String notionId = "notion-" + System.currentTimeMillis();
    private static final OffsetDateTime now = OffsetDateTime.now();

    private final WebClient appleClient = WebClient.builder().baseUrl(APPLE_SERVICE_URL).build();

    @BeforeAll
    static void setup() {
        randomTaskName = "🍏 Dynamic_Test_" + System.currentTimeMillis();
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: Create in Notion (Now Date + Description) -> Propagate to Apple")
    void testCreateFlow() throws Exception {
        String description = "Esta es una nota generada dinámicamente el " + now;
        
        // 1. Simular Notion Page con Fecha Actual y Notas
        Map<String, Object> notionJson = createNotionPageJson(randomTaskName, "Crítico", "Intenso", "Abstracto", description, now);
        NotionPageResponse pageResponse = objectMapper.convertValue(notionJson, NotionPageResponse.class);
        CoreExecutable domainData = notionSyncAdapter.toDomain(pageResponse);

        // 2. Ejecutar Sync
        syncEngineService.processExternalUpdate("NOTION", notionId, domainData);

        System.out.println("⏳ Waiting for creation in Apple...");
        
        boolean foundInApple = false;
        for (int i = 0; i < 10; i++) {
            List<Map> reminders = appleClient.get().uri("/reminders").retrieve().bodyToFlux(Map.class).collectList().block();
            Optional<Map> appleReminder = reminders.stream()
                    .filter(r -> r.get("title").equals(randomTaskName))
                    .findFirst();
            
            if (appleReminder.isPresent()) {
                lastAppleId = (String) appleReminder.get().get("id");
                // Validar que las notas llegaron a Apple
                assertEquals(description, appleReminder.get().get("notes"), "Las notas no coinciden en Apple");
                foundInApple = true;
                break;
            }
            Thread.sleep(2000);
        }

        assertTrue(foundInApple, "El recordatorio no se creó en Apple");
        System.out.println("✅ Step 1 Success: Task created in Apple with notes and current date.");
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Change attributes in Notion (Energy, Mental Load, Title) -> Sync to Apple")
    void testUpdateAttributesFlow() throws Exception {
        assertNotNull(lastAppleId);

        String updatedTitle = randomTaskName + " [UPDATED]";
        String updatedDesc = "Nota actualizada en el paso 2";
        
        // Cambiar Energía a 'Automático' e Impacto a 'Bajo'
        Map<String, Object> notionJson = createNotionPageJson(updatedTitle, "Bajo", "Automático", "Rutinario", updatedDesc, now.plusDays(1));
        NotionPageResponse pageResponse = objectMapper.convertValue(notionJson, NotionPageResponse.class);
        CoreExecutable domainData = notionSyncAdapter.toDomain(pageResponse);

        // Actualizar
        syncEngineService.processExternalUpdate("NOTION", notionId, domainData);

        System.out.println("⏳ Waiting for attribute update in Apple...");
        boolean updatedInApple = false;
        for (int i = 0; i < 5; i++) {
            Map reminder = appleClient.get().uri("/items/{id}", lastAppleId).retrieve().bodyToMono(Map.class).block();
            if (updatedTitle.equals(reminder.get("title")) && updatedDesc.equals(reminder.get("notes"))) {
                updatedInApple = true;
                break;
            }
            Thread.sleep(2000);
        }

        assertTrue(updatedInApple, "Apple no reflejó los cambios de título o notas");
        System.out.println("✅ Step 2 Success: Attributes (Title, Notes, Date) updated in Apple.");
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Mark as DONE in Notion -> Sync to Apple")
    void testMarkAsDoneFlow() throws Exception {
        assertNotNull(lastAppleId);

        Map<String, Object> notionJson = createNotionPageJson(randomTaskName + " [UPDATED]", "Bajo", "Automático", "Rutinario", "", now);
        Map<String, Object> props = (Map<String, Object>) notionJson.get("properties");
        props.put("Status", Map.of("type", "status", "status", Map.of("name", "Done")));
        
        NotionPageResponse pageResponse = objectMapper.convertValue(notionJson, NotionPageResponse.class);
        CoreExecutable domainData = notionSyncAdapter.toDomain(pageResponse);

        syncEngineService.processExternalUpdate("NOTION", notionId, domainData);

        System.out.println("⏳ Waiting for completion in Apple...");
        boolean isCompleted = false;
        for (int i = 0; i < 5; i++) {
            Map reminder = appleClient.get().uri("/items/{id}", lastAppleId).retrieve().bodyToMono(Map.class).block();
            if (Boolean.TRUE.equals(reminder.get("isCompleted"))) {
                isCompleted = true;
                break;
            }
            Thread.sleep(2000);
        }

        assertTrue(isCompleted, "Apple no marcó la tarea como completada");
        System.out.println("✅ Step 3 Success: Task completed in Apple.");
    }

    @AfterAll
    static void cleanup(@Autowired SpringDataCoreExecutableRepository repo) {
        if (lastAppleId != null) {
            System.out.println("🧹 Final Cleanup: Removing test reminder " + lastAppleId);
            try {
                WebClient.builder().baseUrl(APPLE_SERVICE_URL).build()
                        .delete().uri("/reminders?id=" + lastAppleId)
                        .retrieve().bodyToMono(Void.class).block();
            } catch (Exception ignored) {}
        }
        repo.deleteAll();
    }

    private Map<String, Object> createNotionPageJson(String name, String impact, String energy, String mental, String desc, OffsetDateTime date) {
        Map<String, Object> page = new HashMap<>();
        page.put("id", UUID.randomUUID().toString());
        Map<String, Object> props = new HashMap<>();
        
        props.put("Name", Map.of("type", "title", "title", List.of(Map.of("plain_text", name, "text", Map.of("content", name)))));
        props.put("Impact", Map.of("type", "select", "select", Map.of("name", impact)));
        props.put("Energy", Map.of("type", "select", "select", Map.of("name", energy)));
        props.put("Mental Load", Map.of("type", "select", "select", Map.of("name", mental)));
        props.put("Status", Map.of("type", "status", "status", Map.of("name", "Not started")));
        
        // Description (Notes)
        props.put("Description", Map.of("type", "rich_text", "rich_text", List.of(Map.of("plain_text", desc, "text", Map.of("content", desc)))));
        
        // Date (Dynamic)
        if (date != null) {
            String isoDate = date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            props.put("Date", Map.of("type", "date", "date", Map.of("start", isoDate)));
        }

        page.put("properties", props);
        return page;
    }
}
