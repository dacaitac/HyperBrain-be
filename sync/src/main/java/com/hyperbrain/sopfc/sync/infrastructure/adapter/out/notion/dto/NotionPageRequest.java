package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotionPageRequest {
    private Parent parent;
    private Map<String, NotionProperty> properties;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parent {
        @JsonProperty("database_id")
        private String databaseId;
    }
}
