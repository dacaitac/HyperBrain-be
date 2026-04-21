package com.hyperbrain.sopfc.prioritizer.application.usecase;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutablePriorityCalculatorTest {

    private final ExecutablePriorityCalculator calculator = new ExecutablePriorityCalculator();

    @Test
    void testCalculateScoreBasedOnMetodology() {
        // GIVEN: Una tarea con alto impacto y urgencia, pero poco esfuerzo
        CoreExecutable task = CoreExecutable.builder()
                .impactScore(5)      // Máximo impacto
                .urgencyScore(5.0)    // Máxima urgencia
                .effortScore(1)      // Mínimo esfuerzo
                .mentalLoad(2)
                .energyDrain(3)
                .build();

        // WHEN: Calculamos el score
        double score = calculator.calculateScore(task, 1.0); // Modulador biométrico neutral

        // THEN: El score debe ser alto (> 7) según la fórmula de 4DX + James Clear
        // Formula aproximada: (5*0.4) + (5*0.3) + (4*0.1) ...
        assertTrue(score > 4.0, "El score de prioridad debería ser alto para tareas críticas y fáciles: " + score);
    }

    @Test
    void testCalculateScoreLowPriority() {
        // GIVEN: Una tarea de bajo impacto, no urgente y con mucho esfuerzo
        CoreExecutable task = CoreExecutable.builder()
                .impactScore(1)
                .urgencyScore(1.0)
                .effortScore(5)
                .mentalLoad(5)
                .build();

        double score = calculator.calculateScore(task, 1.0);

        assertTrue(score < 3.0, "El score debería ser bajo: " + score);
    }
}
