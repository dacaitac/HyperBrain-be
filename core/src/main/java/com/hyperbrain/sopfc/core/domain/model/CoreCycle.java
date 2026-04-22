package com.hyperbrain.sopfc.core.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa un Ciclo de enfoque (Metas de alto nivel 4DX).
 * Un Ciclo agrupa múltiples ejecutables para medir el ROI de tiempo y energía.
 */
public record CoreCycle(
    UUID id,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    CycleStatus status
) {
    public enum CycleStatus {
        ACTIVE, COMPLETED, ARCHIVED
    }
}
