package com.hyperbrain.sopfc.infrastructure.adapter.out.notion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotionProperty {
    // Eliminado el campo 'type' ya que Notion lo infiere del esquema de la base de datos
    // y tenerlo con valor nulo o incorrecto puede causar 400 Bad Request.
    
    private List<TextContent> title;
    private SelectValue select;
    private StatusValue status;
    private DateValue date;
    private Boolean checkbox;
    private Double number;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextContent {
        private Text text;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Text {
            private String content;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SelectValue {
        private String name;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatusValue {
        private String name;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DateValue {
        private String start;
        private String end;
    }

    public static NotionProperty title(String text) {
        return NotionProperty.builder()
                .title(List.of(new TextContent(new TextContent.Text(text))))
                .build();
    }

    public static NotionProperty select(String name) {
        return NotionProperty.builder()
                .select(new SelectValue(name))
                .build();
    }

    public static NotionProperty status(String name) {
        return NotionProperty.builder()
                .status(new StatusValue(name))
                .build();
    }

    public static NotionProperty checkbox(boolean value) {
        return NotionProperty.builder()
                .checkbox(value)
                .build();
    }

    public static NotionProperty number(double value) {
        return NotionProperty.builder()
                .number(value)
                .build();
    }

    public static NotionProperty date(String start, String end) {
        return NotionProperty.builder()
                .date(new DateValue(start, end))
                .build();
    }
}
