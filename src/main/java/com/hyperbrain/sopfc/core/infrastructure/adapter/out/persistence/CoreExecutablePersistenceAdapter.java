package com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutionProfileEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CoreExecutablePersistenceAdapter implements ExecutableRepositoryPort {

    private final SpringDataCoreExecutableRepository springDataRepository;

    @Override
    public Optional<CoreExecutable> findByIdAndTenantId(UUID id, UUID tenantId) {
        return springDataRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDomain);
    }

    @Override
    public Optional<CoreExecutable> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID tenantId) {
        springDataRepository.deleteById(id);
    }

    @Override
    public CoreExecutable save(CoreExecutable executable) {
        CoreExecutableEntity entity = toEntity(executable);
        
        // Link profile if exists
        if (entity.getExecutionProfile() != null) {
            entity.getExecutionProfile().setExecutable(entity);
        }
        
        CoreExecutableEntity savedEntity = springDataRepository.save(entity);
        return toDomain(savedEntity);
    }

    private CoreExecutable toDomain(CoreExecutableEntity entity) {
        CoreExecutable.CoreExecutableBuilder builder = CoreExecutable.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .parentId(entity.getParentId())
                .cycleId(entity.getCycleId())
                .name(entity.getName())
                .type(entity.getType())
                .status(entity.getStatus())
                .context(entity.getContext())
                .priorityScore(entity.getPriorityScore())
                .isPlanned(entity.isPlanned())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime());

        if (entity.getExecutionProfile() != null) {
            builder.estimatedMinutes(entity.getExecutionProfile().getEstimatedMinutes())
                   .energyDrain(entity.getExecutionProfile().getEnergyDrain())
                   .mentalLoad(entity.getExecutionProfile().getMentalLoad());
        }

        return builder.build();
    }

    private CoreExecutableEntity toEntity(CoreExecutable domain) {
        CoreExecutionProfileEntity profileEntity = CoreExecutionProfileEntity.builder()
                .tenantId(domain.getTenantId())
                .estimatedMinutes(domain.getEstimatedMinutes())
                .energyDrain(domain.getEnergyDrain())
                .mentalLoad(domain.getMentalLoad())
                .build();

        return CoreExecutableEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .parentId(domain.getParentId())
                .cycleId(domain.getCycleId())
                .name(domain.getName())
                .type(domain.getType())
                .status(domain.getStatus())
                .context(domain.getContext())
                .priorityScore(domain.getPriorityScore())
                .isPlanned(domain.isPlanned())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .executionProfile(profileEntity)
                .build();
    }
}
