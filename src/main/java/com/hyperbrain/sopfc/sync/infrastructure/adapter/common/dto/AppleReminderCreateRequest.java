package com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto;
import lombok.*;
import java.time.OffsetDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AppleReminderCreateRequest {
    private String title;
    private String notes;
    private OffsetDateTime dueDate;
    private String calendarName;
    private Boolean isCompleted;
}
