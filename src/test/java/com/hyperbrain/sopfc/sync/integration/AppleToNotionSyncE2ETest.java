package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderCreateRequest;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AppleToNotionSyncE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataCoreExecutableRepository executableRepository;

    @Autowired
    private SyncMappingRepositoryPort syncMappingRepo;

    private static final String APPLE_SERVICE_URL = "http://127.0.0.1:1995";
    private final WebClient appleClient = WebClient.builder().baseUrl(APPLE_SERVICE_URL).build();

    private String lastAppleId;
    private String randomTitle;

    @BeforeEach
    void setup() {
        randomTitle = "🍎 Apple_Source_Test_" + System.currentTimeMillis();
    }

    @Test
    @DisplayName("E2E: Create in Apple -> Java Webhook -> DB Persist -> Prevent Rebound")
    void testAppleToNotionFlow() throws Exception {
        // 1. Crear recordatorio REAL en Apple vía API
        AppleReminderCreateRequest appleReq = AppleReminderCreateRequest.builder()
                .title(randomTitle)
                .notes("Nota desde Apple")
                .calendarName("Reminders")
                .build();

        Map response = appleClient.post().uri("/reminders").bodyValue(appleReq).retrieve().bodyToMono(Map.class).block();
        lastAppleId = (String) response.get("id");
        assertNotNull(lastAppleId, "No se pudo crear el recordatorio en Apple");
        System.out.println("✅ Real reminder created in Apple: " + lastAppleId);

        // 2. Simular que Sentinel detecta el cambio y manda el Webhook a Java
        AppleReminderDto webhookDto = new AppleReminderDto();
        webhookDto.setId(lastAppleId);
        webhookDto.setTitle(randomTitle);
        webhookDto.setNotes("Nota desde Apple");
        webhookDto.setIsCompleted(false);

        mockMvc.perform(post("/api/apple/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookDto)))
                .andExpect(status().isAccepted());

        // 3. Verificar persistencia en Java
        Thread.sleep(2000);
        CoreExecutableEntity entity = executableRepository.findAll().stream()
                .filter(e -> e.getName().equals(randomTitle))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("La tarea de Apple no se sincronizó en la DB de Java"));
        
        System.out.println("✅ Task synced from Apple to local DB: " + entity.getId());

        // 4. VALIDAR PREVENCIÓN DE REBOTE (Checksum)
        // Buscamos el mapeo que se creó para este ejecutable y el sistema APPLE_REMINDERS
        SyncMappingRepositoryPort.SyncMapping mapping = syncMappingRepo.findByExternalId(lastAppleId, "APPLE_REMINDERS")
                .orElseThrow(() -> new RuntimeException("No se encontró el mapeo de identidad para Apple"));
        
        assertNotNull(mapping.lastKnownChecksum(), "El checksum no se generó. El sistema rebotará cambios.");
        System.out.println("✅ Checksum verified: " + mapping.lastKnownChecksum() + ". Rebound prevention is ACTIVE.");
    }

    @AfterEach
    void cleanup() {
        if (lastAppleId != null) {
            System.out.println("🧹 Cleaning up Apple record: " + lastAppleId);
            try {
                appleClient.delete().uri("/reminders?id=" + lastAppleId).retrieve().bodyToMono(Void.class).block();
            } catch (Exception ignored) {}
        }
        executableRepository.deleteAll();
    }
}
