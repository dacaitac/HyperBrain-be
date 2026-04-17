package com.hyperbrain.sopfc.infrastructure.adapter.common.dto;
import lombok.*;
import java.time.OffsetDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AppleEventCreateRequest {
    private String title;
    private String notes;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private String calendarName;
}
