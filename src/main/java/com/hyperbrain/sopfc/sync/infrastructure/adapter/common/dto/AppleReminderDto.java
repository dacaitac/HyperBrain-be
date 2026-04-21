package com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppleReminderDto {
    private String id;
    private String title;
    private String notes;       // Corregido: Añadido campo notes para sincronización
    private String source;      // "Reminders" or "Events"
    private String calendarName;
    private String changeType;  // CREATE, UPDATE, DELETE
    private OffsetDateTime timestamp;
    private OffsetDateTime dueDate;     // For Reminders
    private OffsetDateTime startDate;   // For Events
    private OffsetDateTime endDate;     // For Events
    private Map<String, String> metadata;
    private Boolean isCompleted;
}
