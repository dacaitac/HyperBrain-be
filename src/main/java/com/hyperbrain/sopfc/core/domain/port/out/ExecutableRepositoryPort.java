package com.hyperbrain.sopfc.core.domain.port.out;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import java.util.Optional;
import java.util.UUID;

public interface ExecutableRepositoryPort {
    Optional<CoreExecutable> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<CoreExecutable> findById(UUID id);
    CoreExecutable save(CoreExecutable executable);
    void delete(UUID id, UUID tenantId);
}