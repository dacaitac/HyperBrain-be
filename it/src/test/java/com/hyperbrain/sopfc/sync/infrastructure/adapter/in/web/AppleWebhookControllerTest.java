package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.SopfcApplication;

import com.hyperbrain.sopfc.sync.application.usecase.SyncEngineService;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort.SyncMapping;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto.AppleReminderDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@WebFluxTest(AppleWebhookController.class)
class AppleWebhookControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private SyncEngineService syncEngineService;

    @Test
    void shouldReceiveAppleUpdateAndTriggerSyncEngine() {
        // Given
        String externalId = "apple-123";
        AppleReminderDto payload = AppleReminderDto.builder()
                .id(externalId)
                .title("Test Task")
                .calendarName("Reminders")
                .changeType("UPDATE")
                .isCompleted(true)
                .build();

        // When
        webTestClient.post()
                .uri("/api/v1/sync/apple/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isAccepted();

        // Then
        ArgumentCaptor<CoreExecutable> executableCaptor = ArgumentCaptor.forClass(CoreExecutable.class);
        verify(syncEngineService).processExternalUpdate(eq("APPLE_REMINDERS"), eq(externalId), executableCaptor.capture());
        
        CoreExecutable captured = executableCaptor.getValue();
        assertEquals("Test Task", captured.getName());
        assertEquals(com.hyperbrain.sopfc.core.domain.model.ExecutableStatus.DONE, captured.getStatus());
    }
}
