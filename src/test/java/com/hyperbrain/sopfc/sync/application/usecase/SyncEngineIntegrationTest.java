package com.hyperbrain.sopfc.sync.application.usecase;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.core.domain.model.ExecutionProfile;
import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SyncEngineIntegrationTest {

    private SyncEngineService syncEngine;

    @Autowired
    private ExecutableRepositoryPort localRepo;

    @Autowired
    private SyncMappingRepositoryPort syncMappingRepo;

    @Mock
    private ExternalSyncPort mockNotionPort;

    @Mock
    private ExternalSyncPort mockApplePort;

    @Mock
    private OutboxPort mockOutboxPort;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantId = UUID.randomUUID();
        
        when(mockNotionPort.getSystemIdentifier()).thenReturn("NOTION");
        when(mockApplePort.getSystemIdentifier()).thenReturn("APPLE_REMINDERS");
        
        // Inyectamos manualmente los mocks para evitar conflictos de @MockBean con List<ExternalSyncPort>
        syncEngine = new SyncEngineService(
            List.of(mockNotionPort, mockApplePort),
            localRepo,
            syncMappingRepo,
            mockOutboxPort
        );
    }

    @Test
    void shouldSyncNewExternalItemAndPreventRebounds() {
        // 1. Simular nuevo item desde Notion
        String extId = "notion-123";
        CoreExecutable extItem = CoreExecutable.builder()
                .name("External Task")
                .status(ExecutableStatus.PENDING)
                .tenantId(tenantId)
                .estimatedMinutes(30)
                .energyDrain(2)
                .mentalLoad(2)
                .build();

        when(mockNotionPort.fetchDelta()).thenReturn(List.of(
                new ExternalSyncPort.ExternalSyncResult(extItem, extId)
        ));
        when(mockApplePort.fetchDelta()).thenReturn(List.of());

        // 2. Ejecutar Sync
        System.out.println("--- Initial Sync ---");
        syncEngine.syncAll(tenantId);

        // 3. Validar persistencia local
        SyncMappingRepositoryPort.SyncMapping mapping = syncMappingRepo.findByExternalId(extId, "NOTION")
                .orElseThrow();
        assertNotNull(mapping.executableId());
        
        CoreExecutable local = localRepo.findByIdAndTenantId(mapping.executableId(), tenantId).orElseThrow();
        assertEquals("External Task", local.getName());
        String initialChecksum = mapping.lastKnownChecksum();
        assertNotNull(initialChecksum);

        // 4. Segunda Sincronización (Mismo dato) - Debe prevenir rebote
        System.out.println("--- Rebound Prevention Sync ---");
        syncEngine.syncAll(tenantId);

        SyncMappingRepositoryPort.SyncMapping mappingAfter = syncMappingRepo.findByExternalId(extId, "NOTION").orElseThrow();
        assertEquals(initialChecksum, mappingAfter.lastKnownChecksum(), "Checksum should not change if data is the same");

        // 5. Simular Cambio Externo
        CoreExecutable updatedExtItem = extItem.toBuilder()
                .status(ExecutableStatus.DONE)
                .build();
        
        when(mockNotionPort.fetchDelta()).thenReturn(List.of(
                new ExternalSyncPort.ExternalSyncResult(updatedExtItem, extId)
        ));

        System.out.println("--- Update Sync ---");
        syncEngine.syncAll(tenantId);

        // 6. Validar actualización local
        CoreExecutable localUpdated = localRepo.findByIdAndTenantId(mapping.executableId(), tenantId).orElseThrow();
        assertEquals(ExecutableStatus.DONE, localUpdated.getStatus());
        
        SyncMappingRepositoryPort.SyncMapping mappingFinal = syncMappingRepo.findByExternalId(extId, "NOTION").orElseThrow();
        assertNotEquals(initialChecksum, mappingFinal.lastKnownChecksum(), "Checksum should change after update");
    }
}
