package com.hyperbrain.sopfc.core.application.usecase;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.port.in.MarkExecutableDoneUseCase;
import com.hyperbrain.sopfc.core.domain.exception.ExecutableNotFoundException;
import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

public class MarkExecutableDoneUseCaseImpl implements MarkExecutableDoneUseCase {

    private final ExecutableRepositoryPort repositoryPort;
    private final OutboxPort outboxPort;

    public MarkExecutableDoneUseCaseImpl(ExecutableRepositoryPort repositoryPort, OutboxPort outboxPort) {
        this.repositoryPort = repositoryPort;
        this.outboxPort = outboxPort;
    }

    @Override
    @Transactional
    public CoreExecutable markAsDone(UUID executableId, UUID tenantId) {
        CoreExecutable executable = repositoryPort.findByIdAndTenantId(executableId, tenantId)
                .orElseThrow(() -> new ExecutableNotFoundException(executableId));

        executable.markAsDone();
        
        CoreExecutable savedExecutable = repositoryPort.save(executable);
        
        outboxPort.saveEvent(
            savedExecutable.getTenantId(), 
            "CORE_EXECUTABLE", 
            savedExecutable.getId().toString(), 
            "EXECUTABLE_STATUS_CHANGED", 
            new StatusChangedPayload(savedExecutable.getStatus().name(), "SOPFC_INTERNAL")
        );

        return savedExecutable;
    }

    private record StatusChangedPayload(String newStatus, String sourceSystem) {}
}