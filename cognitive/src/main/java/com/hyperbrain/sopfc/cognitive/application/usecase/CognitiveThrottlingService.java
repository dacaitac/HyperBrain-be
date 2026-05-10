package com.hyperbrain.sopfc.cognitive.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CognitiveThrottlingService {

    private static final int BASE_THRESHOLD = 15;

    /**
     * Throttling Biométrico:
     * Umbral_Tsunami_Dinamico = Umbral_Base (15) * (Readiness_Score / 100)
     */
    public int calculateDynamicThreshold(int readinessScore) {
        int threshold = (int) (BASE_THRESHOLD * (readinessScore / 100.0));
        log.info("🧠 [THROTTLING] Dynamic Cognitive Threshold: {} (Readiness: {})", threshold, readinessScore);
        return threshold;
    }

    public boolean canAcceptMoreLoad(int currentLoad, int readinessScore) {
        return currentLoad < calculateDynamicThreshold(readinessScore);
    }
}
