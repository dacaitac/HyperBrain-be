package com.hyperbrain.sopfc.sync.infrastructure.adapter.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class AppleReminderCreateRequest {
    private String title;
    private String notes;
    private String date;
    private String calendarName;
    private Boolean isCompleted;
    private Integer priority;
    private String url;
}
