package com.hyperbrain.sopfc.core.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExecutionProfile {
    private final int estimatedMinutes;
    private final int mentalLoad;
    private final int energyDrain;

    public ExecutionProfile(int estimatedMinutes, int mentalLoad, int energyDrain) {
        if (estimatedMinutes < 0 || mentalLoad < 0 || energyDrain < 0) {
            throw new IllegalArgumentException("Profile values cannot be negative.");
        }
        this.estimatedMinutes = estimatedMinutes;
        this.mentalLoad = mentalLoad;
        this.energyDrain = energyDrain;
    }
}