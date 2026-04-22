package com.hyperbrain.sopfc.core.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representa la unidad mínima ejecutable del sistema (Tarea, Hábito o Nodo de Aprendizaje).
 * Sigue el patrón Aggregate Root para el dominio Core.
 */
@Getter
@Builder(toBuilder = true)
public class CoreExecutable {
    private final UUID id;
    
    // Jerarquía y Agrupación
    private final UUID parentId;
    private final UUID cycleId;
    
    private String name;
    private String description;
    @Builder.Default
    private ExecutableType type = ExecutableType.TASK;
    @Builder.Default
    private ExecutableStatus status = ExecutableStatus.PENDING;
    private String context;
    
    // Perfil de Ejecución e Inteligencia
    private Double priorityScore;
    private Double urgencyScore;
    private Integer impactScore;
    private Integer effortScore;
    private Integer mentalLoad;
    private Integer energyDrain;
    private Integer estimatedMinutes;

    // Campos extendidos para sincronización bidireccional rica
    private Integer applePriority; // 1-9
    private String externalUrl;
    private OffsetDateTime completionDate;
    private OffsetDateTime lastModifiedDate;

    // Planificación
    private boolean isPlanned;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    public enum ExecutableType {
        TASK, HABIT, LEARNING_NODE, AGENDA, ACTIVITY
    }

    public void markAsDone() {
        if (this.status == ExecutableStatus.DONE) {
            throw new IllegalStateException("Executable is already DONE.");
        }
        this.status = ExecutableStatus.DONE;
        this.completionDate = OffsetDateTime.now();
    }

    public void updatePriorityScore(Double newScore) {
        this.priorityScore = newScore;
    }

    public void plan(OffsetDateTime start, OffsetDateTime end) {
        this.startTime = start;
        this.endTime = end;
        this.isPlanned = true;
    }
}
