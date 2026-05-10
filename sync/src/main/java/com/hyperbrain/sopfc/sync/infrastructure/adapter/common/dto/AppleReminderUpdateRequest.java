package com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class AppleReminderUpdateRequest {
    private String id;
    private String newTitle;
    private String newNotes;
    private String isoStartDate; // Renamed and String
    private String newEndDate; // String
    private Boolean isCompleted;
    private Integer priority;
    private String url;
    private java.util.List<String> alarms;
    private String recurrence;
}
