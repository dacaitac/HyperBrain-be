package com.hyperbrain.sopfc.infrastructure.adapter.out.persistence;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.model.ExecutionProfile;
import com.hyperbrain.sopfc.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.infrastructure.adapter.out.persistence.entity.CoreExecutionProfileEntity;
import com.hyperbrain.sopfc.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Component
public class CoreExecutablePersistenceAdapter implements ExecutableRepositoryPort {

    private final SpringDataCoreExecutableRepository springDataRepository;

    public CoreExecutablePersistenceAdapter(SpringDataCoreExecutableRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<CoreExecutable> findByIdAndTenantId(UUID id, UUID tenantId) {
        return springDataRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID tenantId) {
        springDataRepository.deleteByIdAndTenantId(id, tenantId);
    }

    @Override
    public CoreExecutable save(CoreExecutable executable) {
        // Ensure ID is present for new items to correctly link child entities
        if (executable.getId() == null) {
            executable = executable.toBuilder().id(UUID.randomUUID()).build();
        }
        CoreExecutableEntity entity = toEntity(executable);
        CoreExecutableEntity savedEntity = springDataRepository.save(entity);
        return toDomain(savedEntity);
    }

    private CoreExecutable toDomain(CoreExecutableEntity entity) {
        ExecutionProfile profile = null;
        if (entity.getExecutionProfile() != null) {
            profile = ExecutionProfile.builder()
                    .estimatedMinutes(entity.getExecutionProfile().getEstimatedMinutes())
                    .mentalLoad(entity.getExecutionProfile().getMentalLoad())
                    .energyDrain(entity.getExecutionProfile().getEnergyDrain())
                    .build();
        }

        return CoreExecutable.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .name(entity.getName())
                .status(entity.getStatus())
                .context(entity.getContext())
                .priorityScore(entity.getPriorityScore())
                .impact(entity.getImpact())
                .isPlanned(entity.isPlanned())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .executionProfile(profile)
                .build();
    }

    private CoreExecutableEntity toEntity(CoreExecutable domain) {
        CoreExecutionProfileEntity profileEntity = null;
        if (domain.getExecutionProfile() != null) {
            profileEntity = CoreExecutionProfileEntity.builder()
                    .executableId(domain.getId())
                    .tenantId(domain.getTenantId())
                    .estimatedMinutes(domain.getExecutionProfile().getEstimatedMinutes())
                    .mentalLoad(domain.getExecutionProfile().getMentalLoad())
                    .energyDrain(domain.getExecutionProfile().getEnergyDrain())
                    .build();
        }

        return CoreExecutableEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .name(domain.getName())
                .status(domain.getStatus())
                .context(domain.getContext())
                .priorityScore(domain.getPriorityScore())
                .impact(domain.getImpact())
                .isPlanned(domain.isPlanned())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .executionProfile(profileEntity)
                .build();
    }
}