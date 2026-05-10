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

import org.awaitility.Awaitility;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static com.hyperbrain.sopfc.it.util.ServiceAssumptions.assumeAppleServicesAreActive;
import static org.awaitility.Awaitility.await;
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
        assumeAppleServicesAreActive();
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

        // 2. WEBHOOK to local server
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
        await("Apple Mapping creation").atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> syncMappingRepo.findByExternalId(appleId, "APPLE_SENTINEL").isPresent());

        SyncMappingRepositoryPort.SyncMapping mapping = syncMappingRepo.findByExternalId(appleId, "APPLE_SENTINEL").orElseThrow();
        assertNotNull(mapping.executableId(), "Executable ID should be generated upon sync");

        // 4. VALIDATE NOTION PROPAGATION (Wait up to 20s)
        await("Notion Mapping creation").atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> syncMappingRepo.findByExecutableId(mapping.executableId(), "NOTION").isPresent());
        
        SyncMappingRepositoryPort.SyncMapping notionMapping = syncMappingRepo.findByExecutableId(mapping.executableId(), "NOTION").orElseThrow();

        System.out.println("✅ Multi-system sync flow validated for Executable: " + mapping.executableId());
    }
}

