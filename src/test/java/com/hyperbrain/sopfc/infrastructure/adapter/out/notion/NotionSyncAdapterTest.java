package com.hyperbrain.sopfc.infrastructure.adapter.out.notion;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.infrastructure.adapter.out.notion.dto.NotionPageResponse;
import com.hyperbrain.sopfc.infrastructure.adapter.out.notion.dto.NotionProperty;
import com.hyperbrain.sopfc.infrastructure.adapter.out.notion.dto.NotionQueryResponse;
import com.hyperbrain.sopfc.infrastructure.config.notion.NotionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotionSyncAdapterTest {

    private NotionSyncAdapter adapter;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private NotionProperties properties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        properties = new NotionProperties();
        properties.setToken("fake-token");
        properties.setVersion("2022-06-28");
        properties.setDatabaseIds(Map.of("tasks", "tasks-db-id"));

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        // Mock the new codecs call
        when(webClientBuilder.codecs(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        adapter = new NotionSyncAdapter(webClientBuilder, properties);
    }

    @Test
    void shouldFetchById() {
        // Given
        String externalId = "page-123";
        NotionPageResponse response = NotionPageResponse.builder()
                .id(externalId)
                .properties(Map.of(
                        "Name", NotionProperty.title("Test Task"),
                        "Status", NotionProperty.status("Not started")
                ))
                .build();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/pages/{pageId}"), eq(externalId))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(NotionPageResponse.class)).thenReturn(Mono.just(response));

        // When
        Optional<ExternalSyncPort.ExternalSyncResult> result = adapter.fetchById(externalId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Task", result.get().executable().getName());
        assertEquals(ExecutableStatus.PENDING, result.get().executable().getStatus());
        assertEquals(externalId, result.get().externalId());
    }

    @Test
    void shouldFetchDelta() {
        // Given
        NotionPageResponse res1 = NotionPageResponse.builder()
                .id("1")
                .properties(Map.of("Name", NotionProperty.title("Task 1"), "Status", NotionProperty.status("Not started")))
                .build();
        NotionQueryResponse queryResponse = new NotionQueryResponse();
        queryResponse.setResults(List.of(res1));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/databases/{databaseId}/query"), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        // Mock the mandatory bodyValue for sorts - returns RequestHeadersSpec
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(NotionQueryResponse.class)).thenReturn(Mono.just(queryResponse));

        // When
        List<ExternalSyncPort.ExternalSyncResult> results = adapter.fetchDelta();

        // Then
        assertFalse(results.isEmpty());
        assertEquals("Task 1", results.getFirst().executable().getName());
        assertEquals("1", results.getFirst().externalId());
    }

    @Test
    void shouldMapAllProperties() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        String startStr = now.toString();
        
        NotionPageResponse response = NotionPageResponse.builder()
                .id("page-123")
                .properties(Map.of(
                        "Name", NotionProperty.title("Rich Task"),
                        "Status", NotionProperty.status("Done"),
                        "Complete", NotionProperty.checkbox(true),
                        "Impact", NotionProperty.select("Crítico"),
                        "Date", NotionProperty.date(startStr, null),
                        "Energy", NotionProperty.select("Sostenido"),
                        "Mental Load", NotionProperty.select("Foco")
                ))
                .build();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(NotionPageResponse.class)).thenReturn(Mono.just(response));

        // When
        Optional<ExternalSyncPort.ExternalSyncResult> result = adapter.fetchById("page-123");

        // Then
        assertTrue(result.isPresent());
        CoreExecutable exec = result.get().executable();
        assertEquals("Rich Task", exec.getName());
        assertEquals(ExecutableStatus.DONE, exec.getStatus());
        assertTrue(exec.isPlanned());
        assertEquals(5, exec.getImpact());
        assertNotNull(exec.getStartTime());
        assertEquals(3, exec.getExecutionProfile().getEnergyDrain());
        assertEquals(2, exec.getExecutionProfile().getMentalLoad());
    }
}
