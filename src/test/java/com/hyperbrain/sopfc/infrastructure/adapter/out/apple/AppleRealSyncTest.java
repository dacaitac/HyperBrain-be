package com.hyperbrain.sopfc.infrastructure.adapter.out.apple;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.infrastructure.adapter.common.dto.AppleReminderDto;
import com.hyperbrain.sopfc.infrastructure.config.notion.NotionProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = { "spring.flyway.enabled=false" })
@org.springframework.test.context.ActiveProfiles("test")
class AppleRealSyncTest {

    @Autowired
    private AppleSentinelSyncAdapter appleAdapter;

    /**
     * Este test intenta crear un recordatorio REAL en tu Mac vía ReminderCLI.
     * Solo para validación manual o si el servicio está arriba.
     */
    @Test
    void shouldCreateRealReminderInApple() {
        // Given
        CoreExecutable executable = CoreExecutable.builder()
                .name("Tarea desde HyperBrainV2 Integration Test")
                .status(ExecutableStatus.PENDING)
                .build();

        // When
        String externalId = appleAdapter.pushCreate(executable);

        // Then
        System.out.println("Reminder creado con ID: " + externalId);
        assertNotNull(externalId);
    }
}
