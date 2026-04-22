package com.hyperbrain.sopfc.cognitive.domain.model;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class LearningState {
    UUID id;
    UUID executableId;
    int lastScore;
    int fragileCount;
    OffsetDateTime nextReviewDate;
    String phase; // PRIMING, FRAGILE, CONSOLIDATED
}
