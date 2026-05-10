package com.hyperbrain.sopfc.sync.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SyncEngineServiceTest {

    private SyncEngineService syncEngineService;
    private ExternalSyncPort externalPort;
    private ExecutableRepositoryPort localRepo;
    private SyncMappingRepositoryPort syncMappingRepo;
    private SyncPersistenceService syncPersistenceService;
    private OutboxPort outboxPort;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        externalPort = mock(ExternalSyncPort.class);
        localRepo = mock(ExecutableRepositoryPort.class);
        syncMappingRepo = mock(SyncMappingRepositoryPort.class);
        syncPersistenceService = mock(SyncPersistenceService.class);
        outboxPort = mock(OutboxPort.class);
        objectMapper = new ObjectMapper();
        
        when(externalPort.getSystemIdentifier()).thenReturn("TEST-SYSTEM");

        syncEngineService = new SyncEngineService(List.of(externalPort), localRepo, syncMappingRepo, syncPersistenceService, outboxPort);
    }

    @Test
    void testSyncAll() {
        CoreExecutable executable = CoreExecutable.builder()
                .name("Test")
                .build();
        
        ExternalSyncPort.ExternalSyncResult result = new ExternalSyncPort.ExternalSyncResult(executable, "ext-123");
        when(externalPort.fetchDelta()).thenReturn(List.of(result));
        when(syncMappingRepo.findByExternalId(anyString(), anyString())).thenReturn(Optional.empty());
        when(syncPersistenceService.createFullLinkAtomic(any(), anyString(), anyString())).thenReturn(executable);

        syncEngineService.syncAll();

        // In the new implementation, syncAll calls processExternalUpdate 
        // which eventually calls createFullLinkAtomic or updateFullLinkAtomic
        verify(syncPersistenceService, atLeastOnce()).createFullLinkAtomic(any(), anyString(), anyString());
    }
}
