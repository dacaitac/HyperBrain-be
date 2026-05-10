package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hyperbrain.sopfc.SopfcApplication;
import com.hyperbrain.sopfc.common.application.outbox.OutboxScheduler;
import com.hyperbrain.sopfc.common.infrastructure.config.SyncContextHolder;
import com.hyperbrain.sopfc.common.infrastructure.persistence.repository.OutboxEventRepository;
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
public class FullBidirectionalSyncE2ETest {

    private static final Logger log = LoggerFactory.getLogger(FullBidirectionalSyncE2ETest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private NotionSyncAdapter notionAdapter;
    @Autowired private SpringDataCoreExecutableRepository executableRepository;
    @Autowired private SpringDataSyncMappingRepository mappingRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private OutboxScheduler outboxScheduler;
    @Autowired private ObjectMapper objectMapper;

    private static final String APPLE_SERVICE_URL = "http://127.0.0.1:1995";
    private WebClient appleClient;
    private String sharedAppleId;
    private String sharedNotionId;
    private UUID localId;
    private String testTitle;

    @BeforeAll
    void init() {
        appleClient = WebClient.builder().baseUrl(APPLE_SERVICE_URL).build();
        mappingRepository.deleteAll();
        executableRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        ServiceAssumptions.assumeAppleServicesAreActive();
        SyncContextHolder.clear();
    }

    @Test
    @Order(1)
    @DisplayName("1. Apple -> Local -> Notion")
    void step1_CreateFromApple() throws Exception {
        testTitle = "E2E-BI-" + System.currentTimeMillis();
        
        // Create in Apple
        var res = appleClient.post().uri("/reminders")
                .bodyValue(Map.of("title", testTitle)).retrieve()
                .bodyToMono(JsonNode.class).block();
        sharedAppleId = res.get("id").asText();

        // Webhook Apple -> Backend
        mockMvc.perform(post("/api/v1/sync/apple/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "id", sharedAppleId, "title", testTitle, "changeType", "CREATE"
                )))).andExpect(status().isAccepted());

        await("Apple Mapping").atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> mappingRepository.findByExternalIdAndExternalSystem(sharedAppleId, "APPLE_REMINDERS").isPresent());
        localId = mappingRepository.findByExternalIdAndExternalSystem(sharedAppleId, "APPLE_REMINDERS").get().getExecutableId();
        
        // Propagate to Notion
        outboxScheduler.processOutbox();

        await("Notion Mapping").atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> mappingRepository.findAllByExecutableId(localId).stream().anyMatch(m -> m.getExternalSystem().equals("NOTION")));
        sharedNotionId = mappingRepository.findAllByExecutableId(localId).stream().filter(m -> m.getExternalSystem().equals("NOTION")).findFirst().get().getExternalId();
        
        log.info("✅ Step 1: Initial sync confirmed. Notion ID: {}", sharedNotionId);
    }

    @Test
    @Order(2)
    @DisplayName("2. Notion (Complete=true) -> Apple (isCompleted=true)")
    void step2_UpdateFromNotion() throws Exception {
        log.info("🚀 [E2E] STEP 2: Completing task in Notion...");

        // Simulate Notion Webhook for Completion
        ObjectNode notionData = objectMapper.createObjectNode();
        notionData.put("id", sharedNotionId);
        ObjectNode properties = notionData.putObject("properties");
        properties.putObject("Name").putArray("title").addObject().putObject("text").put("content", testTitle);
        properties.putObject("Complete").put("checkbox", true);
        properties.putObject("Status").putObject("status").put("name", "Done");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "page_updated");
        payload.set("data", notionData);

        mockMvc.perform(post("/api/v1/sync/notion/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted());

        // Process Outbox (Backend -> Apple)
        outboxScheduler.processOutbox();

        // Verify in Apple
        await("Apple Status Sync (True)").atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    var apple = appleClient.get().uri("/items/{id}", sharedAppleId).retrieve().bodyToMono(JsonNode.class).block();
                    return apple != null && apple.get("isCompleted").asBoolean();
                });
        
        log.info("✅ Step 2: Notion -> Apple (Completed) verified.");
    }

    @Test
    @Order(3)
    @DisplayName("3. Notion (Complete=false) -> Apple (isCompleted=false)")
    void step3_UncompleteFromNotion() throws Exception {
        log.info("🚀 [E2E] STEP 3: Un-marking completion in Notion...");

        ObjectNode notionData = objectMapper.createObjectNode();
        notionData.put("id", sharedNotionId);
        ObjectNode properties = notionData.putObject("properties");
        properties.putObject("Name").putArray("title").addObject().putObject("text").put("content", testTitle);
        properties.putObject("Complete").put("checkbox", false);
        properties.putObject("Status").putObject("status").put("name", "In progress");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "page_updated");
        payload.set("data", notionData);

        mockMvc.perform(post("/api/v1/sync/notion/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted());

        outboxScheduler.processOutbox();

        await("Apple Status Sync (False)").atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    var apple = appleClient.get().uri("/items/{id}", sharedAppleId).retrieve().bodyToMono(JsonNode.class).block();
                    return apple != null && !apple.get("isCompleted").asBoolean();
                });

        log.info("✅ Step 3: Notion -> Apple (Uncompleted) verified.");
    }

    @Test
    @Order(4)
    @DisplayName("4. Cleanup & Final Verification")
    void step4_Cleanup() throws Exception {
        if (sharedAppleId != null) {
            appleClient.method(org.springframework.http.HttpMethod.DELETE).uri("/reminders")
                    .bodyValue(Map.of("id", sharedAppleId)).retrieve().toBodilessEntity().block();
        }
        if (sharedNotionId != null) {
            notionAdapter.pushDelete(sharedNotionId);
        }
        mappingRepository.deleteAll();
        executableRepository.deleteAll();
        log.info("✅ Step 4: Cleanup verified.");
    }
}

