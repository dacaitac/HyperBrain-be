package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotionQueryResponse {
    private List<NotionPageResponse> results;
    @Builder.Default
    private boolean hasMore = false;
    private String nextCursor;
}
