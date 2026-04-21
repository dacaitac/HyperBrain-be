package com.hyperbrain.sopfc.core.domain.port.in;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import java.util.UUID;

public interface MarkExecutableDoneUseCase {
    CoreExecutable markAsDone(UUID executableId, UUID tenantId);
}