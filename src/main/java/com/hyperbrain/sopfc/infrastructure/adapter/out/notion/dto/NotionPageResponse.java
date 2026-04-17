package com.hyperbrain.sopfc.infrastructure.adapter.out.notion.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotionPageResponse {
    private String id;
    private Map<String, NotionProperty> properties;
}
