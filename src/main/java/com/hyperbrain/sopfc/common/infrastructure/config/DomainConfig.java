package com.hyperbrain.sopfc.common.infrastructure.config;

import com.hyperbrain.sopfc.core.application.usecase.MarkExecutableDoneUseCaseImpl;
import com.hyperbrain.sopfc.core.domain.port.in.MarkExecutableDoneUseCase;
import com.hyperbrain.sopfc.common.domain.port.out.OutboxPort;
import com.hyperbrain.sopfc.core.domain.port.out.ExecutableRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public MarkExecutableDoneUseCase markExecutableDoneUseCase(
            ExecutableRepositoryPort repositoryPort,
            OutboxPort outboxPort) {
        
        // Inyectamos las implementaciones de infraestructura (adaptadores out) 
        // en el caso de uso del dominio, cumpliendo el principio de inversión de dependencias.
        return new MarkExecutableDoneUseCaseImpl(repositoryPort, outboxPort);
    }
}