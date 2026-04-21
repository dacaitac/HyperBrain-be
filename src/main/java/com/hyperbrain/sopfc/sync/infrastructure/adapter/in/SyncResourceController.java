package com.hyperbrain.sopfc.sync.infrastructure.adapter.in;

import com.hyperbrain.sopfc.sync.domain.port.in.SyncResourceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncResourceController {

    private final SyncResourceUseCase syncResourceUseCase;

    @PostMapping("/create")
    public ResponseEntity<Void> create(@RequestBody Map<String, Object> data) {
        log.info("[Sync] Recibida petición de creación con {} campos", data.size());
        syncResourceUseCase.create(data);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update")
    public ResponseEntity<Void> update(@RequestBody Map<String, Object> data) {
        log.info("[Sync] Recibida petición de actualización con {} campos", data.size());
        syncResourceUseCase.update(data);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody Map<String, Object> data) {
        log.info("[Sync] Recibida petición de eliminación con {} campos", data.size());
        syncResourceUseCase.delete(data);
        return ResponseEntity.ok().build();
    }
}
