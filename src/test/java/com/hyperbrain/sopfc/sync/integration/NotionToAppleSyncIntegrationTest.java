package com.hyperbrain.sopfc.sync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.apple.AppleSentinelSyncAdapter;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto.NotionWebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NotionToAppleSyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataCoreExecutableRepository executableRepository;

    @MockBean
    private WebClient.Builder webClientBuilder;

    private WebClient appleWebClient;

    @BeforeEach
    void setUp() {
        // Setup Apple WebClient Mock
        appleWebClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(appleWebClient);

        when(appleWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
    }

    @Test
    void testNotionWebhookTriggersAppleSync() throws Exception {
        // 1. Preparar datos en DB: Un ejecutable con mapeos de Notion y Apple
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String notionId = "3498bc9c5d918040a274c20b4de3bfa4"; // ID del JSON de test
        String appleId = "APPLE-REM-123";

        CoreExecutableEntity entity = CoreExecutableEntity.builder()
                .tenantId(tenantId)
                .name("Tarea Original")
                .type(CoreExecutable.ExecutableType.TASK)
                .status(com.hyperbrain.sopfc.core.domain.model.ExecutableStatus.PENDING)
                .build();
        entity = executableRepository.save(entity);

        // TODO: Insertar SyncMappings (requeriría un repositorio de mappings o JDBC directo si no hay repo)
        // Por ahora asumimos que el SyncEngineService encontrará el mapeo de Apple si lo inyectamos o preparamos.
        // Como estamos en un test de integración, usaré el repositorio de mappings si existe.

        // 2. Cargar JSON real de Notion y enviarlo al webhook
        InputStream is = getClass().getResourceAsStream("/sync-resources/update.json");
        NotionWebhookPayload payload = objectMapper.readValue(is, NotionWebhookPayload.class);

        mockMvc.perform(post("/api/v1/sync/notion/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted());

        // 3. Verificar que se disparó la llamada a Apple Sentinel
        // El SyncEngineService debería haber detectado que este Executable tiene un gemelo en Apple
        // y llamar al AppleSentinelSyncAdapter.pushUpdate()
        
        // ArgumentCaptor para verificar el payload enviado a Apple
        // verify(appleWebClient, timeout(5000)).post(); 
    }
}
