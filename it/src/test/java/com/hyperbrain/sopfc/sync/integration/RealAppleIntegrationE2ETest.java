package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.SopfcApplication;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.NotionSyncAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SopfcApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RealAppleIntegrationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotionSyncAdapter notionSyncAdapter;

    @Autowired
    private SpringDataCoreExecutableRepository executableRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        com.hyperbrain.sopfc.it.util.ServiceAssumptions.assumeAppleServicesAreActive();
        executableRepository.deleteAll();
    }

    @Test
    void shouldHandleFullSyncFlowFromAppleToNotion() throws Exception {
        String notionId = UUID.randomUUID().toString();
        when(notionSyncAdapter.pushCreate(any(CoreExecutable.class))).thenReturn(notionId);
        
        String appleId = "apple-" + UUID.randomUUID();
        
        String applePayload = """
            {
                "id": "%s",
                "title": "Task from Apple",
                "isCompleted": false
            }
        """.formatted(appleId);

        mockMvc.perform(post("/api/v1/sync/apple/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(applePayload))
                .andExpect(status().isAccepted());

        assertFalse(executableRepository.findAll().isEmpty());
    }
}
