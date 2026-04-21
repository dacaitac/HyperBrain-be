package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotionWebhookPayload {
    private String type;
    private NotionPageResponse data;
}
