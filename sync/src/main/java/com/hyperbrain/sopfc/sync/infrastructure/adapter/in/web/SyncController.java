package com.hyperbrain.sopfc.sync.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.sync.application.usecase.SyncEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncEngineService syncEngineService;

    @PostMapping("/all")
    public ResponseEntity<Void> triggerSyncAll() {
        log.info("📢 [HTTP] Manual sync triggered.");
        syncEngineService.syncAll();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/executable/{id}")
    public ResponseEntity<Void> deleteExecutable(@PathVariable UUID id) {
        log.info("📢 [HTTP] Manual delete triggered for ID: {}", id);
        syncEngineService.processLocalDelete(id);
        return ResponseEntity.accepted().build();
    }
}
