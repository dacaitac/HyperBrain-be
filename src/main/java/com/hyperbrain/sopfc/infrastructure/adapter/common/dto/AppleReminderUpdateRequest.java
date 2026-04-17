package com.hyperbrain.sopfc.infrastructure.adapter.common.dto;
import lombok.*;
import java.time.OffsetDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AppleReminderUpdateRequest {
    private String id;
    private String newTitle;
    private String newNotes;
    private OffsetDateTime newDueDate;
    private Boolean isCompleted;
}
