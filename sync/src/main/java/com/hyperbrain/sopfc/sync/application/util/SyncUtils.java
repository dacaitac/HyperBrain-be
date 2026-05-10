package com.hyperbrain.sopfc.sync.application.util;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import java.util.Objects;

public class SyncUtils {

    public static String normalizeNotionId(String id) {
        if (id == null) return null;
        return id.replace("-", "").toLowerCase();
    }

    /**
     * Calcula un checksum robusto que incluye fechas para detectar cambios de planificación.
     */
    public static String calculateChecksum(CoreExecutable executable) {
        if (executable == null) return "";
        return String.valueOf(Objects.hash(
                normalizeString(executable.getName()),
                normalizeString(executable.getDescription()),
                executable.getStatus(),
                executable.getApplePriority(),
                executable.getExternalUrl(),
                executable.isPlanned(),
                normalizeDate(executable.getStartTime()),
                normalizeDate(executable.getEndTime())
        ));
    }

    private static java.time.OffsetDateTime normalizeDate(java.time.OffsetDateTime dt) {
        if (dt == null) return null;
        // Truncamos a segundos para evitar ruido de nanosegundos entre sistemas
        return dt.withNano(0);
    }

    private static String normalizeString(String input) {
        return input == null ? "" : input.trim();
    }
}
