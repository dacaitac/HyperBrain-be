package com.hyperbrain.sopfc.core.application.usecase;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.port.in.MarkExecutableDoneUseCase;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class MarkExecutableDoneUseCaseImpl implements MarkExecutableDoneUseCase {

    private final ExecutableRepositoryPort repositoryPort;

    @Override
    public CoreExecutable markAsDone(UUID executableId) {
        log.info("🏁 [USE-CASE] Marking executable as DONE: {}", executableId);
        
        CoreExecutable executable = repositoryPort.findById(executableId)
                .orElseThrow(() -> new IllegalArgumentException("Executable not found: " + executableId));

        executable.markAsDone();
        
        // Al guardar en el repositorio, el Entity Listener disparará automáticamente el evento al Outbox
        return repositoryPort.save(executable);
    }
}
