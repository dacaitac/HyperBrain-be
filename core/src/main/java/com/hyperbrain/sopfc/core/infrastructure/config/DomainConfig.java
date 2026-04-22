package com.hyperbrain.sopfc.core.infrastructure.config;

import com.hyperbrain.sopfc.core.application.usecase.MarkExecutableDoneUseCaseImpl;
import com.hyperbrain.sopfc.core.domain.port.in.MarkExecutableDoneUseCase;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public MarkExecutableDoneUseCase markExecutableDoneUseCase(
            ExecutableRepositoryPort repositoryPort) {

        // Inyectamos las implementaciones de infraestructura (adaptadores out)
        // en el caso de uso del dominio, cumpliendo el principio de inversión de dependencias.
        // El Outbox ahora es manejado automáticamente por el CoreExecutableEntityListener.
        return new MarkExecutableDoneUseCaseImpl(repositoryPort);
    }
}
