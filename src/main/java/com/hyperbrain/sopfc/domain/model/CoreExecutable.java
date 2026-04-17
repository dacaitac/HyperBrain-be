package com.hyperbrain.sopfc.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class CoreExecutable {
    private final UUID id;
    private final UUID tenantId;
    private String name;
    private ExecutableStatus status;
    private String context;
    private Double priorityScore;
    private Integer impact;
    private boolean isPlanned;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private ExecutionProfile executionProfile;

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