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

        adapter = new NotionSyncAdapter(builder, props, objectMapper);
    }

    @Test
    void testMappingFromCreateJson() throws Exception {
        InputStream is = getClass().getResourceAsStream("/sync-resources/create.json");
        Map<String, Object> payload = objectMapper.readValue(is, Map.of().getClass());
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        NotionPageResponse response = objectMapper.convertValue(data, NotionPageResponse.class);

        CoreExecutable domain = adapter.toDomain(response);

        assertNotNull(domain);
        assertEquals("444.", domain.getName());
        assertEquals(CoreExecutable.ExecutableType.TASK, domain.getType());
        // Status from JSON is "Not started"
        assertEquals(ExecutableStatus.PENDING, domain.getStatus());
        
        // Selects (defaults in adapter when null in JSON)
        assertEquals(3, domain.getImpactScore()); 
        assertEquals(2, domain.getMentalLoad()); 
    }

    @Test
    void testMappingFromUpdateJson() throws Exception {
        InputStream is = getClass().getResourceAsStream("/sync-resources/update.json");
        Map<String, Object> payload = objectMapper.readValue(is, Map.of().getClass());
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        NotionPageResponse response = objectMapper.convertValue(data, NotionPageResponse.class);

        CoreExecutable domain = adapter.toDomain(response);

        assertNotNull(domain);
        assertEquals("444.", domain.getName());
        assertFalse(domain.isPlanned());
    }
}
