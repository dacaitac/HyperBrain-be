package com.hyperbrain.sopfc.sync.infrastructure.adapter.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.sync.domain.port.out.SyncResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileSystemSyncResourceRepository implements SyncResourceRepository {

    private final ObjectMapper objectMapper;
    private final String resourceDir = "src/main/resources/sync-resources";

    @Override
    public void save(String fileName, Map<String, Object> data) {
        log.debug("[Persistence] Intentando guardar recurso en archivo: {}.json", fileName);
        try {
            Path path = Paths.get(resourceDir, fileName + ".json");
            File file = path.toFile();
            
            if (!file.getParentFile().exists()) {
                log.info("[Persistence] Creando directorio de recursos: {}", resourceDir);
                file.getParentFile().mkdirs();
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
            log.info("[Persistence] Recurso {} guardado exitosamente en: {}", fileName, path.toAbsolutePath());
        } catch (IOException e) {
            log.error("[Persistence] ERROR CRÍTICO al guardar el recurso {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Error al guardar el recurso sync: " + fileName, e);
        }
    }
}
