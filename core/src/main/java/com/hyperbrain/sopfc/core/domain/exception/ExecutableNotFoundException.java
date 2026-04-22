package com.hyperbrain.sopfc.core.domain.exception;

import com.hyperbrain.sopfc.common.domain.exception.DomainException;
import java.util.UUID;

public class ExecutableNotFoundException extends DomainException {
    public ExecutableNotFoundException(UUID id) {
        super("Executable not found: " + id);
    }
}
