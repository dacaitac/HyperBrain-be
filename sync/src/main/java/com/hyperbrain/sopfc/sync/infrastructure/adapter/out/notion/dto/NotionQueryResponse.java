package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotionQueryResponse {
    private List<NotionPageResponse> results;
    private boolean hasMore;
    private String nextCursor;
}
