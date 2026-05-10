package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderDto;
import com.hyperbrain.sopfc.SopfcApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SopfcApplication.class)
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

    @BeforeEach
    void setUp() {
        com.hyperbrain.sopfc.it.util.ServiceAssumptions.assumeAppleServicesAreActive();
        executableRepository.deleteAll();
    }

    @Test
    void shouldSyncAppleReminderToNotionViaWebhook() throws Exception {
        String externalId = "apple-" + UUID.randomUUID();
        
        AppleReminderDto reminderDto = AppleReminderDto.builder()
            .id(externalId)
            .title("Task from Apple")
            .isCompleted(false)
            .build();

        mockMvc.perform(post("/api/v1/sync/apple/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminderDto)))
                .andExpect(status().isAccepted()); // Controller uses accepted() for async processing

        // Verify creation in DB (might need a small retry or just check existence since it's local DB)
        List<CoreExecutableEntity> entities = executableRepository.findAll();
        assertFalse(entities.isEmpty(), "Should have created at least one executable");
        
        CoreExecutableEntity entity = entities.stream()
                .filter(e -> e.getName().equals("Task from Apple"))
                .findFirst()
                .orElseThrow();
        
        assertTrue(syncMappingRepo.findByExternalId(externalId, "APPLE_REMINDERS").isPresent());
    }
}
