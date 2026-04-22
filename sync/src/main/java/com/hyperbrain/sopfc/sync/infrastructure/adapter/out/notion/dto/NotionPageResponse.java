package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotionPageResponse {
    private String id;
    
    @JsonProperty("archived")
    private boolean archived;

    private Map<String, NotionProperty> properties;

    public boolean isArchived() {
        return archived;
    }
}
