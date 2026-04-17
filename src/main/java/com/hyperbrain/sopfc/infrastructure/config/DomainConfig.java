package com.hyperbrain.sopfc.infrastructure.config;

import com.hyperbrain.sopfc.application.usecase.MarkExecutableDoneUseCaseImpl;
import com.hyperbrain.sopfc.domain.port.in.MarkExecutableDoneUseCase;
import com.hyperbrain.sopfc.domain.port.out.EventPublisherPort;
import com.hyperbrain.sopfc.domain.port.out.ExecutableRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public MarkExecutableDoneUseCase markExecutableDoneUseCase(
            ExecutableRepositoryPort repositoryPort,
            EventPublisherPort eventPublisherPort) {
        
        // Inyectamos las implementaciones de infraestructura (adaptadores out) 
        // en el caso de uso del dominio, cumpliendo el principio de inversión de dependencias.
        return new MarkExecutableDoneUseCaseImpl(repositoryPort, eventPublisherPort);
    }
}