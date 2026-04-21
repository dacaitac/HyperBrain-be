package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto.NotionPageResponse;
import com.hyperbrain.sopfc.sync.infrastructure.config.notion.NotionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotionSyncAdapterTest {

    private NotionSyncAdapter adapter;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        WebClient.Builder builder = Mockito.mock(WebClient.Builder.class);
        Mockito.when(builder.baseUrl(Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.defaultHeader(Mockito.anyString(), Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.codecs(Mockito.any())).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(Mockito.mock(WebClient.class));

        NotionProperties props = new NotionProperties();
        props.setToken("dummy");
        props.setVersion("2022-06-28");
        props.setDatabaseIds(Map.of("tasks", "dummy-db"));

        adapter = new NotionSyncAdapter(builder, props);
    }

    @Test
    void testMappingFromCreateJson() throws Exception {
        // Cargar JSON real de Notion (vía webhook)
        InputStream is = getClass().getResourceAsStream("/sync-resources/create.json");
        Map<String, Object> payload = objectMapper.readValue(is, Map.of().getClass());
        
        // El JSON de automatización envuelve la página en "data"
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        NotionPageResponse response = objectMapper.convertValue(data, NotionPageResponse.class);

        // Act
        CoreExecutable domain = adapter.toDomain(response);

        // Assert (Mapeo "Perfecto" basado en los campos del JSON real)
        assertNotNull(domain);
        assertEquals("444.", domain.getName());
        assertEquals(CoreExecutable.ExecutableType.TASK, domain.getType());
        assertEquals(ExecutableStatus.PENDING, domain.getStatus());
        
        // Fórmulas
        assertEquals(1.0, domain.getPriorityScore());
        assertEquals(1.0, domain.getUrgencyScore());
        assertEquals(1, domain.getEffortScore());
        assertEquals(15, domain.getEstimatedMinutes());

        // Selects
        assertEquals(3, domain.getImpactScore()); // Default por null
        assertEquals(2, domain.getMentalLoad()); // Default por null
        
        // Relaciones (Solo IDs de Notion por ahora)
        // Nota: En el JSON real, las relaciones están vacías en el "create.json" inicial
        assertNull(domain.getParentId());
        assertNull(domain.getCycleId());
    }

    @Test
    void testMappingFromUpdateJsonWithHierarchy() throws Exception {
        InputStream is = getClass().getResourceAsStream("/sync-resources/update.json");
        Map<String, Object> payload = objectMapper.readValue(is, Map.of().getClass());
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        NotionPageResponse response = objectMapper.convertValue(data, NotionPageResponse.class);

        CoreExecutable domain = adapter.toDomain(response);

        assertNotNull(domain);
        assertEquals("444.", domain.getName());
        // Validar que se extraen metadatos de Notion
        assertFalse(domain.isPlanned()); // Date es null en el JSON recibido
    }
}
