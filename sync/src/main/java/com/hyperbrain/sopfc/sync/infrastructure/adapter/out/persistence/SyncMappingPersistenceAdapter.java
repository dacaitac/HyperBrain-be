package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence;

import com.hyperbrain.sopfc.sync.domain.port.out.SyncMappingRepositoryPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.entity.SyncMappingEntity;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.repository.SpringDataSyncMappingRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SyncMappingPersistenceAdapter implements SyncMappingRepositoryPort {

    private final SpringDataSyncMappingRepository repository;

    public SyncMappingPersistenceAdapter(SpringDataSyncMappingRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SyncMapping> findByExternalId(String externalId, String externalSystem) {
        return repository.findByExternalIdAndExternalSystem(externalId, externalSystem)
                .map(this::toDomain);
    }

    @Override
    public Optional<SyncMapping> findByExecutableId(UUID executableId, String externalSystem) {
        return repository.findByExecutableIdAndExternalSystem(executableId, externalSystem)
                .map(this::toDomain);
    }

    @Override
    public java.util.List<SyncMapping> findAllByExecutableId(UUID executableId) {
        return repository.findAllByExecutableId(executableId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public java.util.List<SyncMapping> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
        repository.flush();
    }

    @Override
    public void save(SyncMapping mapping) {
        SyncMappingEntity entity = toEntity(mapping);
        // Si el ID es nulo, lo generamos manualmente ya que quitamos @GeneratedValue
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        repository.saveAndFlush(entity);
    }

    @Override
    public void delete(SyncMapping mapping) {
        repository.delete(toEntity(mapping));
        repository.flush();
    }

    private SyncMapping toDomain(SyncMappingEntity entity) {
        return new SyncMapping(
                entity.getId(),
                entity.getExecutableId(),
                entity.getExternalSystem(),
                entity.getExternalId(),
                entity.getLastKnownChecksum(),
                entity.getLastSyncedAt(),
                entity.getSyncStatus()
        );
    }

    private SyncMappingEntity toEntity(SyncMapping domain) {
        return SyncMappingEntity.builder()
                .id(domain.id())
                .executableId(domain.executableId())
                .externalSystem(domain.externalSystem())
                .externalId(domain.externalId())
                .lastKnownChecksum(domain.lastKnownChecksum())
                .lastSyncedAt(domain.lastSyncedAt())
                .syncStatus(domain.syncStatus())
                .build();
    }
}
