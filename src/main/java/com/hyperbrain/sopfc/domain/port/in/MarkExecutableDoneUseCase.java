package com.hyperbrain.sopfc.domain.port.in;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import java.util.UUID;

public interface MarkExecutableDoneUseCase {
    CoreExecutable markAsDone(UUID executableId, UUID tenantId);
}