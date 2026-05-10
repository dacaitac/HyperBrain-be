package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.SopfcApplication;
import com.hyperbrain.sopfc.common.application.outbox.OutboxScheduler;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.it.util.ServiceAssumptions;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.repository.SpringDataSyncMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SopfcApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NotionToAppleSyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataCoreExecutableRepository executableRepository;

    @Autowired
    private SpringDataSyncMappingRepository mappingRepository;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @BeforeEach
    void setUp() {
        ServiceAssumptions.assumeAppleServicesAreActive();
        mappingRepository.deleteAll();
        executableRepository.deleteAll();
    }

    @Test
    void shouldHandleNotionWebhookAndSyncToApple() throws Exception {
        String notionPayload = """
            {
                "pageId": "notion-page-123",
                "data": {
                    "id": "notion-page-123",
                    "properties": {
                        "Name": { "title": [ { "text": { "content": "Notion Task" } } ] },
                        "Status": { "status": { "name": "Done" } }
                    }
                }
            }
        """;

        mockMvc.perform(post("/api/v1/sync/notion/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(notionPayload))
                .andExpect(status().isAccepted());

        // Manually trigger outbox
        outboxScheduler.processOutbox();
    }
}
