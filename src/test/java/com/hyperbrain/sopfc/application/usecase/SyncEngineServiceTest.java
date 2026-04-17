package com.hyperbrain.sopfc.application.usecase;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.domain.port.out.EventPublisherPort;
import com.hyperbrain.sopfc.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.domain.port.out.SyncMappingRepositoryPort.SyncMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SyncEngineServiceTest {

    private SyncEngineService syncEngineService;

    @Mock
    private ExternalSyncPort externalPort;

    @Mock
    private ExecutableRepositoryPort localRepo;

    @Mock
    private SyncMappingRepositoryPort syncMappingRepo;

    @Mock
    private EventPublisherPort eventPublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        syncEngineService = new SyncEngineService(List.of(externalPort), localRepo, syncMappingRepo, eventPublisher);
        when(externalPort.getSystemIdentifier()).thenReturn("NOTION");
    }

    @Test
    void shouldCreateNewLocalExecutableWhenNoMappingExists() {
        // Given
        String externalId = "notion-123";
        CoreExecutable extItem = CoreExecutable.builder()
                .name("New External Task")
                .status(ExecutableStatus.PENDING)
                .build();
        
        when(externalPort.fetchDelta()).thenReturn(List.of(
                new ExternalSyncPort.ExternalSyncResult(extItem, externalId)
        ));
        when(syncMappingRepo.findByExternalId(externalId, "NOTION")).thenReturn(Optional.empty());
        when(localRepo.save(any())).thenAnswer(invocation -> {
            CoreExecutable saved = invocation.getArgument(0);
            return saved.toBuilder().id(UUID.randomUUID()).build();
        });

        // When
        syncEngineService.syncAll(UUID.randomUUID());

        // Then
        verify(localRepo).save(argThat(exec -> exec.getName().equals("New External Task")));
        verify(syncMappingRepo).save(argThat(mapping -> mapping.externalId().equals(externalId)));
    }

    @Test
    void shouldUpdateExistingLocalExecutableWhenMappingExistsAndChecksumDiffers() {
        // Given
        UUID localId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String externalId = "notion-123";
        
        SyncMapping mapping = new SyncMapping(
                UUID.randomUUID(), tenantId, localId, "NOTION", externalId, "old-checksum", OffsetDateTime.now(), "IN_SYNC"
        );
        
        CoreExecutable localItem = CoreExecutable.builder()
                .id(localId)
                .tenantId(tenantId)
                .name("Old Name")
                .status(ExecutableStatus.PENDING)
                .build();

        CoreExecutable updatedExtItem = CoreExecutable.builder()
                .name("Updated Name")
                .status(ExecutableStatus.DONE)
                .build();

        when(externalPort.fetchDelta()).thenReturn(List.of(
                new ExternalSyncPort.ExternalSyncResult(updatedExtItem, externalId)
        ));
        when(syncMappingRepo.findByExternalId(externalId, "NOTION")).thenReturn(Optional.of(mapping));
        when(localRepo.findByIdAndTenantId(localId, tenantId)).thenReturn(Optional.of(localItem));

        // When
        syncEngineService.syncAll(tenantId);

        // Then
        verify(localRepo).save(argThat(exec -> exec.getName().equals("Updated Name") && exec.getStatus() == ExecutableStatus.DONE));
        verify(syncMappingRepo).save(argThat(m -> m.externalId().equals(externalId) && !m.lastKnownChecksum().equals("old-checksum")));
    }

    @Test
    void shouldNotUpdateWhenChecksumIsSame() {
        // Given
        UUID localId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String externalId = "notion-123";
        
        CoreExecutable extItem = CoreExecutable.builder()
                .name("Same Name")
                .status(ExecutableStatus.PENDING)
                .build();
        
        // Calculate expected checksum
        String checksum = String.valueOf(java.util.Objects.hash(
                extItem.getName(), extItem.getStatus(), extItem.isPlanned(), extItem.getImpact(),
                extItem.getStartTime(), extItem.getEndTime(), 0, 0
        ));

        SyncMapping mapping = new SyncMapping(
                UUID.randomUUID(), tenantId, localId, "NOTION", externalId, checksum, OffsetDateTime.now(), "IN_SYNC"
        );
        
        when(externalPort.fetchDelta()).thenReturn(List.of(
                new ExternalSyncPort.ExternalSyncResult(extItem, externalId)
        ));
        when(syncMappingRepo.findByExternalId(externalId, "NOTION")).thenReturn(Optional.of(mapping));
        when(localRepo.findByIdAndTenantId(localId, tenantId)).thenReturn(Optional.of(CoreExecutable.builder().id(localId).name("Same Name").build()));

        // When
        syncEngineService.syncAll(tenantId);

        // Then
        verify(localRepo, never()).save(any());
    }
}
