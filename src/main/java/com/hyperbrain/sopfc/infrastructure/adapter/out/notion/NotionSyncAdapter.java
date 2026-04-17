package com.hyperbrain.sopfc.infrastructure.adapter.out.notion;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.domain.model.ExecutionProfile;
import com.hyperbrain.sopfc.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.infrastructure.adapter.out.notion.dto.*;
import com.hyperbrain.sopfc.infrastructure.config.notion.NotionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NotionSyncAdapter implements ExternalSyncPort {

    private final WebClient webClient;
    private final NotionProperties notionProperties;
    private final String tasksDbId;

    public NotionSyncAdapter(WebClient.Builder webClientBuilder, NotionProperties notionProperties) {
        this.notionProperties = notionProperties;
        this.webClient = webClientBuilder
                .baseUrl("https://api.notion.com/v1")
                .defaultHeader("Authorization", "Bearer " + notionProperties.getToken())
                .defaultHeader("Notion-Version", notionProperties.getVersion())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
        this.tasksDbId = notionProperties.getDatabaseIds().get("tasks");
    }

    @Override
    public Optional<ExternalSyncResult> fetchById(String externalId) {
        log.info("🔍 [NOTION-FETCH] Fetching page by ID: {}", externalId);
        return webClient.get()
                .uri("/pages/{pageId}", externalId)
                .retrieve()
                .bodyToMono(NotionPageResponse.class)
                .map(response -> new ExternalSyncResult(toDomain(response), response.getId()))
                .doOnSuccess(res -> log.info("✅ [NOTION-FETCH] Successfully retrieved page: {}", externalId))
                .doOnError(e -> log.error("❌ [NOTION-FETCH] Failed to fetch page {}: {}", externalId, e.getMessage()))
                .blockOptional();
    }

    @Override
    public List<ExternalSyncResult> fetchDelta() {
        log.info("📋 [NOTION-DELTA] Querying database for updates: {}", tasksDbId);
        Map<String, Object> queryBody = Map.of(
            "sorts", List.of(
                Map.of("timestamp", "last_edited_time", "direction", "descending")
            )
        );

        return webClient.post()
                .uri("/databases/{databaseId}/query", tasksDbId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(queryBody)
                .retrieve()
                .bodyToMono(NotionQueryResponse.class)
                .map(response -> response.getResults().stream()
                        .map(res -> new ExternalSyncResult(toDomain(res), res.getId()))
                        .collect(Collectors.toList()))
                .doOnSuccess(list -> log.info("✅ [NOTION-DELTA] Retrieved {} potentially updated items", list.size()))
                .block();
    }

    @Override
    public void pushUpdate(CoreExecutable executable, String externalId) {
        log.info("📤 [NOTION-UPDATE] Pushing update for page: {}", externalId);
        Map<String, NotionProperty> properties = toNotionProperties(executable);
        webClient.patch()
                .uri("/pages/{pageId}", externalId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("properties", properties))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("✅ [NOTION-UPDATE] Successfully updated Notion page: {}", externalId))
                .doOnError(e -> log.error("❌ [NOTION-UPDATE] Failed to update Notion page {}: {}", externalId, e.getMessage()))
                .subscribe();
    }

    @Override
    public String pushCreate(CoreExecutable executable) {
        log.info("➕ [NOTION-CREATE] Creating new page for: '{}'", executable.getName());
        NotionPageRequest request = NotionPageRequest.builder()
                .parent(new NotionPageRequest.Parent(tasksDbId))
                .properties(toNotionProperties(executable))
                .build();

        NotionPageResponse response = webClient.post()
                .uri("/pages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NotionPageResponse.class)
                .doOnSuccess(res -> log.info("✅ [NOTION-CREATE] Page created successfully. NotionID: {}", res.getId()))
                .block();

        return response != null ? response.getId() : null;
    }

    @Override
    public void pushDelete(String externalId) {
        log.info("🗑️ [NOTION-DELETE] Archiving page: {}", externalId);
        webClient.patch()
                .uri("/pages/{pageId}", externalId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("archived", true))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("✅ [NOTION-DELETE] Successfully archived page: {}", externalId))
                .subscribe();
    }

    @Override
    public String getSystemIdentifier() {
        return "NOTION";
    }

    public CoreExecutable toDomain(NotionPageResponse res) {
        Map<String, NotionProperty> props = res.getProperties();
        
        String name = "";
        NotionProperty nameProp = props.get("Name");
        if (nameProp != null && nameProp.getTitle() != null && !nameProp.getTitle().isEmpty()) {
            name = nameProp.getTitle().getFirst().getText().getContent();
        }

        ExecutableStatus status = ExecutableStatus.PENDING;
        NotionProperty statusProp = props.get("Status");
        if (statusProp != null && statusProp.getStatus() != null) {
            status = mapNotionStatusToDomain(statusProp.getStatus().getName());
        }

        OffsetDateTime start = null;
        OffsetDateTime end = null;
        NotionProperty dateProp = props.get("Date");
        if (dateProp != null && dateProp.getDate() != null) {
            if (dateProp.getDate().getStart() != null) {
                start = parseNotionDate(dateProp.getDate().getStart());
            }
            if (dateProp.getDate().getEnd() != null) {
                end = parseNotionDate(dateProp.getDate().getEnd());
            }
        }

        Integer impact = 3;
        NotionProperty impactProp = props.get("Impact");
        if (impactProp != null && impactProp.getSelect() != null) {
            impact = switch (impactProp.getSelect().getName()) {
                case "Crítico" -> 5;
                case "Alto" -> 4;
                case "Medio" -> 3;
                case "Bajo" -> 2;
                case "Mínimo" -> 1;
                default -> 3;
            };
        }

        int energy = 3;
        NotionProperty energyProp = props.get("Energy");
        if (energyProp != null && energyProp.getSelect() != null) {
            energy = switch (energyProp.getSelect().getName()) {
                case "Frenético" -> 5;
                case "Intenso" -> 4;
                case "Sostenido" -> 3;
                case "Ligero" -> 2;
                case "Pasivo" -> 1;
                default -> 3;
            };
        }

        int mental = 2;
        NotionProperty mentalProp = props.get("Mental Load");
        if (mentalProp != null && mentalProp.getSelect() != null) {
            mental = switch (mentalProp.getSelect().getName()) {
                case "Deep Work" -> 5;
                case "Análisis" -> 4;
                case "Coordinación" -> 3;
                case "Foco" -> 2;
                case "Shallow" -> 1;
                default -> 2;
            };
        }

        com.hyperbrain.sopfc.domain.model.ExecutionProfile profile = com.hyperbrain.sopfc.domain.model.ExecutionProfile.builder()
                .energyDrain(energy)
                .mentalLoad(mental)
                .build();

        return CoreExecutable.builder()
                .name(name)
                .status(status)
                .startTime(start)
                .endTime(end)
                .impact(impact)
                .executionProfile(profile)
                .isPlanned(start != null)
                .build();
    }

    private OffsetDateTime parseNotionDate(String dateStr) {
        try {
            if (dateStr.length() == 10) {
                return java.time.LocalDate.parse(dateStr).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            }
            return OffsetDateTime.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, NotionProperty> toNotionProperties(CoreExecutable executable) {
        Map<String, NotionProperty> props = new java.util.HashMap<>();
        props.put("Name", NotionProperty.title(executable.getName()));
        props.put("Status", NotionProperty.status(mapDomainStatusToNotion(executable.getStatus())));
        props.put("Complete", NotionProperty.checkbox(executable.getStatus() == ExecutableStatus.DONE));

        if (executable.getStartTime() != null) {
            String start = executable.getStartTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String end = executable.getEndTime() != null ? executable.getEndTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
            props.put("Date", NotionProperty.date(start, end));
        }
        return props;
    }

    private ExecutableStatus mapNotionStatusToDomain(String notionStatus) {
        return switch (notionStatus) {
            case "In progress" -> ExecutableStatus.IN_PROGRESS;
            case "Done" -> ExecutableStatus.DONE;
            default -> ExecutableStatus.PENDING;
        };
    }

    private String mapDomainStatusToNotion(ExecutableStatus status) {
        return switch (status) {
            case IN_PROGRESS -> "In progress";
            case DONE -> "Done";
            default -> "Not started";
        };
    }
}
