package com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutableEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity.CoreExecutionProfileEntity;
import com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.repository.SpringDataCoreExecutableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CoreExecutablePersistenceAdapter implements ExecutableRepositoryPort {

    private final SpringDataCoreExecutableRepository repository;

    @Override
    public Optional<CoreExecutable> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public CoreExecutable save(CoreExecutable executable) {
        CoreExecutableEntity entity = toEntity(executable);
        if (entity.getExecutionProfile() != null) {
            entity.getExecutionProfile().setExecutable(entity);
        }
        CoreExecutableEntity saved = repository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
        repository.flush();
    }

    private CoreExecutable toDomain(CoreExecutableEntity entity) {
        return CoreExecutable.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .cycleId(entity.getCycleId())
                .name(entity.getName())
                .description(entity.getDescription())
                .context(entity.getContext())
                .status(entity.getStatus())
                .priorityScore(entity.getPriorityScore())
                .type(entity.getType())
                .estimatedMinutes(entity.getExecutionProfile() != null ? entity.getExecutionProfile().getEstimatedMinutes() : null)
                .energyDrain(entity.getExecutionProfile() != null ? entity.getExecutionProfile().getEnergyDrain() : null)
                .mentalLoad(entity.getExecutionProfile() != null ? entity.getExecutionProfile().getMentalLoad() : null)
                .impactScore(entity.getImpactScore())
                .isPlanned(entity.isPlanned())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .applePriority(entity.getApplePriority())
                .externalUrl(entity.getExternalUrl())
                .completionDate(entity.getCompletionDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private CoreExecutableEntity toEntity(CoreExecutable domain) {
        CoreExecutionProfileEntity profileEntity = null;
        if (domain.getEstimatedMinutes() != null || domain.getEnergyDrain() != null || domain.getMentalLoad() != null) {
            profileEntity = CoreExecutionProfileEntity.builder()
                    .executable_id(domain.getId())
                    .estimatedMinutes(domain.getEstimatedMinutes())
                    .energyDrain(domain.getEnergyDrain())
                    .mentalLoad(domain.getMentalLoad())
                    .build();
        }

        return CoreExecutableEntity.builder()
                .id(domain.getId())
                .parentId(domain.getParentId())
                .cycleId(domain.getCycleId())
                .name(domain.getName())
                .description(domain.getDescription())
                .context(domain.getContext())
                .status(domain.getStatus())
                .priorityScore(domain.getPriorityScore())
                .type(domain.getType())
                .impactScore(domain.getImpactScore())
                .isPlanned(domain.isPlanned())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .applePriority(domain.getApplePriority())
                .externalUrl(domain.getExternalUrl())
                .completionDate(domain.getCompletionDate())
                .lastModifiedDate(domain.getLastModifiedDate())
                .executionProfile(profileEntity)
                .build();
    }
}
