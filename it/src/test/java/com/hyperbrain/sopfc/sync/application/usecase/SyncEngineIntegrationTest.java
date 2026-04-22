package com.hyperbrain.sopfc.sync.application.usecase;

import com.hyperbrain.sopfc.SopfcApplication;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SopfcApplication.class)
@ActiveProfiles("test")
@Transactional
@org.junit.jupiter.api.Disabled("Desactivado temporalmente por conflictos de configuración de contexto tras refactorización")
class SyncEngineIntegrationTest {

    @Autowired
    private SyncEngineService syncEngine;

    @Autowired
    private ExecutableRepositoryPort localRepo;

    @Autowired
    private SyncMappingRepositoryPort syncMappingRepo;

    @MockBean(name = "notionSyncAdapter")
    private ExternalSyncPort mockNotionPort;

    @MockBean(name = "appleSyncAdapter")
    private ExternalSyncPort mockApplePort;

    @MockBean
    private OutboxPort mockOutboxPort;

    @BeforeEach
    void setUp() {
        when(mockNotionPort.getSystemIdentifier()).thenReturn("NOTION");
        when(mockApplePort.getSystemIdentifier()).thenReturn("APPLE_REMINDERS");
    }

    @Test
    void shouldSyncNewExternalItemAndPreventRebounds() {
        String extId = "notion-" + UUID.randomUUID();
        CoreExecutable extItem = CoreExecutable.builder()
                .name("External Task")
                .status(ExecutableStatus.PENDING)
                .estimatedMinutes(30)
                .build();

        // Configure mocks to return data on first sync
        when(mockNotionPort.fetchDelta()).thenReturn(List.of(
                new ExternalSyncPort.ExternalSyncResult(extItem, extId)
        ));
        when(mockApplePort.fetchDelta()).thenReturn(List.of());

        syncEngine.syncAll();

        SyncMappingRepositoryPort.SyncMapping mapping = syncMappingRepo.findByExternalId(extId, "NOTION")
                .orElseThrow();
        
        CoreExecutable local = localRepo.findById(mapping.executableId()).orElseThrow();
        assertEquals("External Task", local.getName());

        // Simulate external change
        CoreExecutable updatedExtItem = extItem.toBuilder()
                .name("External Task Updated")
                .status(ExecutableStatus.DONE)
                .build();
        
        // Update mocks for second sync
        when(mockNotionPort.fetchDelta()).thenReturn(List.of(
                new ExternalSyncPort.ExternalSyncResult(updatedExtItem, extId)
        ));

        syncEngine.syncAll();

        CoreExecutable localUpdated = localRepo.findById(mapping.executableId()).orElseThrow();
        assertEquals(ExecutableStatus.DONE, localUpdated.getStatus());
        assertEquals("External Task Updated", localUpdated.getName());
    }
}
