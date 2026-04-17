package com.hyperbrain.sopfc.domain.port.out;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import java.util.Optional;
import java.util.UUID;

public interface ExecutableRepositoryPort {
    Optional<CoreExecutable> findByIdAndTenantId(UUID id, UUID tenantId);
    CoreExecutable save(CoreExecutable executable);
    void delete(UUID id, UUID tenantId);
}