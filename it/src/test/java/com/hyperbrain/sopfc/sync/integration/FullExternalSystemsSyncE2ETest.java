package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.SopfcApplication;
import com.hyperbrain.sopfc.common.application.outbox.OutboxScheduler;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.it.util.ServiceAssumptions;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FullExternalSystemsSyncE2ETest {

    private static final Logger log = LoggerFactory.getLogger(FullExternalSystemsSyncE2ETest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private NotionSyncAdapter notionAdapter;
    @Autowired private SpringDataCoreExecutableRepository executableRepository;
    @Autowired private SpringDataSyncMappingRepository mappingRepository;
    @Autowired private OutboxScheduler outboxScheduler;
    @Autowired private ObjectMapper objectMapper;

    private static final String APPLE_SERVICE_URL = "http://127.0.0.1:1995";
    private WebClient appleClient;
    private String sharedAppleId;
    private String sharedNotionId;
    private String testTitle;

    @BeforeAll
    void init() {
        appleClient = WebClient.builder().baseUrl(APPLE_SERVICE_URL).build();
        mappingRepository.deleteAll();
        executableRepository.deleteAll();
    }

    @BeforeEach
    void checkDependencies() {
        ServiceAssumptions.assumeAppleServicesAreActive();
    }

    @Test
    @Order(1)
    @DisplayName("Apple -> Local -> Notion")
    void step1_CreateAndPropagate() throws Exception {
        testTitle = "E2E-EXT-" + System.currentTimeMillis();
        
        var res = appleClient.post().uri("/reminders")
                .bodyValue(Map.of("title", testTitle)).retrieve()
                .bodyToMono(JsonNode.class).block();
        sharedAppleId = res.get("id").asText();

        mockMvc.perform(post("/api/v1/sync/apple/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "id", sharedAppleId, "title", testTitle, "changeType", "CREATE"
                )))).andExpect(status().isAccepted());

        await("Mapping creation").atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> mappingRepository.findByExternalIdAndExternalSystem(sharedAppleId, "APPLE_REMINDERS").isPresent());

        UUID localId = mappingRepository.findByExternalIdAndExternalSystem(sharedAppleId, "APPLE_REMINDERS").get().getExecutableId();

        outboxScheduler.processOutbox();

        await("Notion propagation").atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> mappingRepository.findAllByExecutableId(localId).stream()
                        .anyMatch(m -> m.getExternalSystem().equals("NOTION")));
        
        sharedNotionId = mappingRepository.findAllByExecutableId(localId).stream()
                .filter(m -> m.getExternalSystem().equals("NOTION")).findFirst().get().getExternalId();
        
        assertTrue(notionAdapter.fetchById(sharedNotionId).isPresent());
    }

    @Test
    @Order(2)
    @DisplayName("Cleanup Verification")
    void step2_CleanupAndVerification() throws Exception {
        if (sharedAppleId != null) {
            appleClient.method(org.springframework.http.HttpMethod.DELETE).uri("/reminders")
                    .bodyValue(Map.of("id", sharedAppleId)).retrieve().toBodilessEntity().block();
        }
        if (sharedNotionId != null) {
            notionAdapter.pushDelete(sharedNotionId);
        }

        mappingRepository.deleteAll();
        executableRepository.deleteAll();

        // Verify no residual test data in externals
        try {
            appleClient.get().uri("/items/{id}", sharedAppleId).retrieve().toBodilessEntity().block();
            fail("Residual data in Apple");
        } catch (Exception ignored) {}

        assertTrue(notionAdapter.isArchived(sharedNotionId), "Residual data in Notion");
        log.info("✅ Cleanup verified.");
    }
}

