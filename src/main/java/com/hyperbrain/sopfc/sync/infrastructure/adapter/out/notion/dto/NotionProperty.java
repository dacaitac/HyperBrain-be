package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotionProperty {
    private String id;
    private String type;
    private List<TextWrapper> title;
    @JsonProperty("rich_text")
    private List<TextWrapper> richText;
    private SelectWrapper select;
    private StatusWrapper status;
    private DateWrapper date;
    private FormulaWrapper formula;
    private List<RelationWrapper> relation;
    private Boolean checkbox;
    private Double number;
    private String url;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextWrapper {
        private String type;
        private TextContent text;
        @JsonProperty("plain_text")
        private String plainText;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TextContent {
            private String content;
            private Link link;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Link {
                private String url;
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectWrapper {
        private String id;
        private String name;
        private String color;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusWrapper {
        private String id;
        private String name;
        private String color;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateWrapper {
        private String start;
        private String end;
        @JsonProperty("time_zone")
        private String timeZone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaWrapper {
        private String type;
        private Double number;
        private String string;
        private Boolean booleanValue;
        private DateWrapper date;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationWrapper {
        private String id;
    }

    // Static Factory Methods for Requests
    public static NotionProperty title(String text) {
        return NotionProperty.builder()
                .title(List.of(new TextWrapper("text", new TextWrapper.TextContent(text, null), text)))
                .build();
    }

    public static NotionProperty status(String name) {
        return NotionProperty.builder()
                .status(new StatusWrapper(null, name, null))
                .build();
    }

    public static NotionProperty select(String name) {
        return NotionProperty.builder()
                .select(new SelectWrapper(null, name, null))
                .build();
    }

    public static NotionProperty checkbox(boolean value) {
        return NotionProperty.builder()
                .checkbox(value)
                .build();
    }

    public static NotionProperty date(String start, String end) {
        return NotionProperty.builder()
                .date(new DateWrapper(start, end, null))
                .build();
    }

    public static NotionProperty relation(String id) {
        return NotionProperty.builder()
                .relation(List.of(new RelationWrapper(id)))
                .build();
    }
}
