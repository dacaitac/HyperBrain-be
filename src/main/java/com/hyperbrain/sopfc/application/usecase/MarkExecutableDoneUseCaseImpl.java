package com.hyperbrain.sopfc.application.usecase;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.port.in.MarkExecutableDoneUseCase;
import com.hyperbrain.sopfc.domain.port.out.EventPublisherPort;
import com.hyperbrain.sopfc.domain.port.out.ExecutableRepositoryPort;
import java.util.UUID;

public class MarkExecutableDoneUseCaseImpl implements MarkExecutableDoneUseCase {

    private final ExecutableRepositoryPort repositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public MarkExecutableDoneUseCaseImpl(ExecutableRepositoryPort repositoryPort, EventPublisherPort eventPublisherPort) {
        this.repositoryPort = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public CoreExecutable markAsDone(UUID executableId, UUID tenantId) {
        CoreExecutable executable = repositoryPort.findByIdAndTenantId(executableId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Executable not found or does not belong to tenant"));

        executable.markAsDone();
        
        CoreExecutable savedExecutable = repositoryPort.save(executable);
        
        eventPublisherPort.publishExecutableStatusChanged(savedExecutable.getId(), savedExecutable.getTenantId(), savedExecutable.getStatus().name(), "SOPFC_INTERNAL");

        return savedExecutable;
    }
}