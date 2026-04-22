package com.hyperbrain.sopfc.prioritizer.application.usecase;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class ExecutablePriorityCalculator {

    /**
     * Ecuación de Priorización Reactiva:
     * S(n, t) = [W(n) * β(B)] + [U(n) / (1 + e^-k(t - t_deadline))] - ((E + M) / 2 * 0.2)
     */
    public double calculateScore(CoreExecutable executable, double biometricModulator) {
        double impact = executable.getImpactScore() != null ? executable.getImpactScore() : 3.0;
        
        // W(n): Peso del esfuerzo (Impacto 35% + Importancia 15% + Inverso del Esfuerzo)
        double weight = (impact * 0.5); 

        // U(n): Urgencia (curva sigmoide asintótica hacia el deadline)
        double urgency = executable.getUrgencyScore() != null ? executable.getUrgencyScore() : calculateUrgency(executable.getEndTime());

        // E, M: Penalización por Alta Energía (E) y Carga Mental (M)
        double effortPenalty = 0.0;
        if (executable.getEnergyDrain() != null || executable.getMentalLoad() != null) {
            double e = executable.getEnergyDrain() != null ? executable.getEnergyDrain() : 3.0;
            double m = executable.getMentalLoad() != null ? executable.getMentalLoad() : 2.0;
            effortPenalty = ((e + m) / 2.0) * 0.2;
        }

        double score = (weight * biometricModulator) + urgency - effortPenalty;
        
        log.debug("📊 [PRIORITY] Calculated score for {}: {} (W={}, U={}, P={})", 
            executable.getId(), score, weight, urgency, effortPenalty);
        
        return score;
    }

    private double calculateUrgency(OffsetDateTime deadline) {
        if (deadline == null) return 0.0;
        
        long hoursToDeadline = ChronoUnit.HOURS.between(OffsetDateTime.now(), deadline);
        
        // Función Sigmoide: 1 / (1 + exp(-k * (t_diff)))
        // k=0.1 para una pendiente suave
        double k = 0.1;
        return 10.0 / (1.0 + Math.exp(k * (hoursToDeadline - 24)));
    }
}
