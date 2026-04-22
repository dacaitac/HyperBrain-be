package com.hyperbrain.sopfc.sync.application.usecase;

import com.hyperbrain.sopfc.SopfcApplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;

@SpringBootTest(classes = SopfcApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CrossSystemSyncIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private SyncMappingRepositoryPort syncMappingRepo;

    private final String vaporUrl = "http://127.0.0.1:1995";
    private final ObjectMapper mapper = new ObjectMapper();
    private WebClient webClient;

    @BeforeEach
    void setup() {
        com.hyperbrain.sopfc.it.util.ServiceAssumptions.assumeAppleServicesAreActive();
        this.webClient = WebClient.builder().build();
    }

    @Test
    @DisplayName("Basic Multi-System Flow: Apple -> Backend -> Notion Mapping")
    void shouldCreateMappingWhenAppleReminderArrives() throws Exception {
        String testTitle = "Build-Test-" + System.currentTimeMillis();

        // 1. CREATE in Apple (Vapor)
        Map<String, Object> createReq = Map.of(
                "title", testTitle,
                "calendarName", "Reminders",
                "isCompleted", false);

        String createRes = webClient.post().uri(vaporUrl + "/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createReq).retrieve()
                .bodyToMono(String.class)
                .block();
        
        assertNotNull(createRes, "Response from Apple Sync Bridge (Vapor) should not be null");
        String appleId = mapper.readTree(createRes).get("id").asText();

        // 2. WEBHOOK to local server (Simulating Apple Shortcut/Event)
        webClient.post()
                .uri("http://localhost:" + port + "/api/v1/sync/apple/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        Map.of(
                                "id", appleId,
                                "title", testTitle,
                                "source", "reminders",
                                "changeType", "CREATE",
                                "isCompleted", false))
                .retrieve()
                .toBodilessEntity()
                .block();

        // 3. VALIDATE INTERNAL MAPPING (Wait up to 10s)
        assertTrue(waitFor(() -> syncMappingRepo.findByExternalId(appleId, "APPLE_REMINDERS").isPresent(), 10000),
                "Mapping for Apple ID was not created in local DB");

        SyncMappingRepositoryPort.SyncMapping mapping = syncMappingRepo.findByExternalId(appleId, "APPLE_REMINDERS").orElseThrow();
        assertNotNull(mapping.executableId(), "Executable ID should be generated upon sync");

        // 4. VALIDATE NOTION PROPAGATION (Logic to verify Notion Mapping was also created)
        assertTrue(waitFor(() -> syncMappingRepo.findByExecutableId(mapping.executableId(), "NOTION").isPresent(), 5000),
                "Mapping for Notion should be automatically created via sync engine");
        
        System.out.println("✅ Multi-system sync flow validated for Executable: " + mapping.executableId());
    }

    private boolean waitFor(BooleanSupplier condition, int timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(1000);
        }
        return false;
    }
}
