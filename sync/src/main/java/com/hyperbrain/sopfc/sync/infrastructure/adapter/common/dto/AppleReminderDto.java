package com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppleReminderDto {
    private String id;
    private String title;
    private String source; // "Reminders" o "Events"
    private String notes;
    private String calendarName;
    private OffsetDateTime dueDate;
    private Boolean isCompleted;
    private String changeType; // CREATE, UPDATE, DELETE
    private Integer priority;
    private String url;
    private java.util.List<OffsetDateTime> alarms;
    private String recurrence;
    private OffsetDateTime completionDate;
    private OffsetDateTime lastModifiedDate;
}
