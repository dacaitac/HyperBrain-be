package com.hyperbrain.sopfc.sync.application.usecase;

import com.hyperbrain.sopfc.sync.domain.port.in.SyncResourceUseCase;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncResourceService implements SyncResourceUseCase {

    private final SyncResourceRepository repository;

    @Override
    public void create(Map<String, Object> data) {
        log.info("[Sync] Procesando recurso para creación");
        repository.save("create", data);
    }

    @Override
    public void update(Map<String, Object> data) {
        log.info("[Sync] Procesando recurso para actualización");
        repository.save("update", data);
    }

    @Override
    public void delete(Map<String, Object> data) {
        log.info("[Sync] Procesando recurso para eliminación");
        repository.save("delete", data);
    }
}
