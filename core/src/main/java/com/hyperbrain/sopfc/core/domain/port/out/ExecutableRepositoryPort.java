package com.hyperbrain.sopfc.core.domain.port.out;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import java.util.Optional;
import java.util.UUID;

public interface ExecutableRepositoryPort {
    Optional<CoreExecutable> findById(UUID id);
    java.util.List<CoreExecutable> findByName(String name);
    java.util.List<CoreExecutable> findByNameAndDate(String name, java.time.OffsetDateTime date);
    CoreExecutable save(CoreExecutable executable);
    void delete(UUID id);
}