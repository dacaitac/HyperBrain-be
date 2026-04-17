package com.hyperbrain.sopfc.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.infrastructure.adapter.common.dto.AppleReminderDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:inbound_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class AppleInboundDatabaseTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ExecutableRepositoryPort localRepo;

    @Autowired
    private SyncMappingRepositoryPort syncMappingRepo;

    @Test
    void shouldCreateNewExecutableInDatabaseWhenNewAppleWebhookArrives() {
        // Given
        String newAppleId = "apple-new-uuid-" + UUID.randomUUID();
        AppleReminderDto payload = AppleReminderDto.builder()
                .id(newAppleId)
                .title("Tarea Creada en iOS")
                .source("Reminders")
                .changeType("CREATE")
                .isCompleted(false)
                .build();

        // When
        webTestClient.post()
                .uri("/api/v1/sync/apple/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isAccepted();

        // Then: Verificar que existe el mapeo y el ejecutable en la base de datos
        var mappingOpt = syncMappingRepo.findByExternalId(newAppleId, "APPLE_REMINDERS");
        assertTrue(mappingOpt.isPresent(), "Debería haberse creado un mapeo para el nuevo ID de Apple.");
        
        var mapping = mappingOpt.get();
        var executableOpt = localRepo.findByIdAndTenantId(mapping.executableId(), mapping.tenantId());
        assertTrue(executableOpt.isPresent(), "Debería haberse creado el CoreExecutable en la base de datos.");
        
        CoreExecutable created = executableOpt.get();
        assertEquals("Tarea Creada en iOS", created.getName());
        assertEquals(com.hyperbrain.sopfc.domain.model.ExecutableStatus.PENDING, created.getStatus());
        
        System.out.println("✅ Validación Exitosa: El ejecutable fue creado en la base de datos del proyecto.");
    }
}
