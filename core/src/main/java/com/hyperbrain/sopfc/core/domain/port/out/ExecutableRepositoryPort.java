package com.hyperbrain.sopfc.core.domain.port.out;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import java.util.Optional;
import java.util.UUID;

public interface ExecutableRepositoryPort {
    Optional<CoreExecutable> findById(UUID id);
    CoreExecutable save(CoreExecutable executable);
    void delete(UUID id);
}