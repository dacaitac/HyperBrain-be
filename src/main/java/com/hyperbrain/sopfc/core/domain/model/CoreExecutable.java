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
    private final UUID tenantId;
    
    // Jerarquía y Agrupación
    private final UUID parentId;
    private final UUID cycleId;
    
    private String name;
    private String description; // De rich_text en Notion
    private ExecutableType type;
    private ExecutableStatus status;
    private String context;
    
    // Perfil de Ejecución e Inteligencia
    private Double priorityScore;
    private Double urgencyScore; // De fórmula "Urgence" en Notion
    private Integer impactScore;
    private Integer effortScore;
    private Integer mentalLoad;
    private Integer energyDrain; // De select "Energy" en Notion
    private Integer estimatedMinutes;

    // Planificación
    private boolean isPlanned;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    public enum ExecutableType {
        TASK, HABIT, LEARNING_NODE
    }

    public void markAsDone() {
        if (this.status == ExecutableStatus.DONE) {
            throw new IllegalStateException("Executable is already DONE.");
        }
        this.status = ExecutableStatus.DONE;
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
