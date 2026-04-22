package com.hyperbrain.sopfc.planner.application.usecase;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AgendaPlannerService {

    /**
     * Agenda Planner (Bin-packing Estocástico):
     * 1. Mapa de disponibilidad.
     * 2. Margen de caos (20%).
     * 3. Asignación por Energía/Carga Mental.
     */
    public List<PlannedTimeBlock> planDay(List<CoreExecutable> prioritizedTasks, OffsetDateTime dayStart) {
        log.info("🗓️ [PLANNER] Planning {} tasks for {}", prioritizedTasks.size(), dayStart.toLocalDate());
        
        List<PlannedTimeBlock> plan = new ArrayList<>();
        OffsetDateTime currentPointer = dayStart.plusHours(8); // Start at 8 AM
        
        for (CoreExecutable task : prioritizedTasks) {
            int duration = task.getEstimatedMinutes() != null ? task.getEstimatedMinutes() : 60;

            // Aplicar Margen de Caos (20% extra de buffer)
            int bufferedDuration = (int) (duration * 1.2);
            
            PlannedTimeBlock block = new PlannedTimeBlock(
                task.getId(),
                currentPointer,
                currentPointer.plusMinutes(duration),
                bufferedDuration
            );
            
            plan.add(block);
            currentPointer = currentPointer.plusMinutes(bufferedDuration);
            
            log.debug("📦 [PLANNER] Packed task {}: {} to {} (Buffer: {} min)", 
                task.getId(), block.start, block.end, bufferedDuration);
        }
        
        return plan;
    }

    public record PlannedTimeBlock(java.util.UUID executableId, OffsetDateTime start, OffsetDateTime end, int bufferedMinutes) {}
}
