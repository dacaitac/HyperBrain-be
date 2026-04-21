package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable.ExecutableType;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto.*;
import com.hyperbrain.sopfc.sync.infrastructure.config.notion.NotionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
                .block();
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
                .block();
    }

    @Override
    public String getSystemIdentifier() {
        return "NOTION";
    }

    public CoreExecutable toDomain(NotionPageResponse res) {
        Map<String, NotionProperty> props = res.getProperties();
        
        // Atributos Básicos
        String name = Optional.ofNullable(props.get("Name"))
                .map(p -> p.getTitle().isEmpty() ? "" : p.getTitle().getFirst().getText().getContent())
                .orElse("");

        String description = Optional.ofNullable(props.get("Description"))
                .map(p -> p.getRichText().isEmpty() ? "" : p.getRichText().getFirst().getText().getContent())
                .orElse("");

        ExecutableType type = mapNotionType(props.get("Type"));
        ExecutableStatus status = mapNotionStatus(props.get("Status"));

        // Atributos de Inteligencia (Fórmulas y Selects)
        Double priorityScore = Optional.ofNullable(props.get("Priority Score"))
                .map(p -> p.getFormula() != null ? p.getFormula().getNumber() : 0.0)
                .orElse(0.0);

        Double urgencyScore = Optional.ofNullable(props.get("Urgence"))
                .map(p -> p.getFormula() != null ? p.getFormula().getNumber() : 0.0)
                .orElse(0.0);

        Integer impact = mapSelectToImpact(props.get("Impact"));
        Integer energy = mapSelectToEnergy(props.get("Energy"));
        
        Integer effort = Optional.ofNullable(props.get("Effort"))
                .map(p -> p.getFormula() != null ? p.getFormula().getNumber().intValue() : 1)
                .orElse(1);

        Integer mental = mapSelectToMental(props.get("Mental Load"));
        
        Integer estimatedMinutes = Optional.ofNullable(props.get("Total Estimate Duration"))
                .map(p -> p.getFormula() != null ? p.getFormula().getNumber().intValue() : 0)
                .orElse(0);

        // Relaciones
        UUID parentId = extractFirstRelation(props.get("Parent Task"));
        UUID cycleId = extractFirstRelation(props.get("Cycle"));

        // Fechas de Planificación
        OffsetDateTime start = null;
        OffsetDateTime end = null;
        NotionProperty dateProp = props.get("Date");
        if (dateProp != null && dateProp.getDate() != null) {
            start = parseNotionDate(dateProp.getDate().getStart());
            end = parseNotionDate(dateProp.getDate().getEnd());
        }

        return CoreExecutable.builder()
                .name(name)
                .description(description)
                .type(type)
                .status(status)
                .priorityScore(priorityScore)
                .urgencyScore(urgencyScore)
                .impactScore(impact)
                .energyDrain(energy)
                .effortScore(effort)
                .mentalLoad(mental)
                .estimatedMinutes(estimatedMinutes)
                .parentId(parentId)
                .cycleId(cycleId)
                .startTime(start)
                .endTime(end)
                .isPlanned(start != null)
                .build();
    }

    private Integer mapSelectToEnergy(NotionProperty prop) {
        if (prop == null || prop.getSelect() == null) return 3;
        return switch (prop.getSelect().getName()) {
            case "Intenso" -> 5;
            case "Exigente" -> 4;
            case "Sostenido" -> 3;
            case "Ejecución" -> 2;
            case "Automático" -> 1;
            default -> 3;
        };
    }

    private ExecutableType mapNotionType(NotionProperty prop) {
        if (prop == null || prop.getSelect() == null) return ExecutableType.TASK;
        return switch (prop.getSelect().getName()) {
            case "Habit", "Routine" -> ExecutableType.HABIT;
            case "Learning", "Analysis" -> ExecutableType.LEARNING_NODE;
            default -> ExecutableType.TASK;
        };
    }

    private ExecutableStatus mapNotionStatus(NotionProperty prop) {
        if (prop == null || prop.getStatus() == null) return ExecutableStatus.PENDING;
        return switch (prop.getStatus().getName()) {
            case "In progress" -> ExecutableStatus.IN_PROGRESS;
            case "Done" -> ExecutableStatus.DONE;
            default -> ExecutableStatus.PENDING;
        };
    }

    private Integer mapSelectToImpact(NotionProperty prop) {
        if (prop == null || prop.getSelect() == null) return 3;
        return switch (prop.getSelect().getName()) {
            case "Crítico" -> 5;
            case "Alto" -> 4;
            case "Moderado" -> 3;
            case "Bajo" -> 2;
            case "Irrelevante" -> 1;
            default -> 3;
        };
    }

    private Integer mapSelectToMental(NotionProperty prop) {
        if (prop == null || prop.getSelect() == null) return 2;
        return switch (prop.getSelect().getName()) {
            case "Abstracto" -> 5;
            case "Complejo" -> 4;
            case "Análisis" -> 3;
            case "Foco" -> 2;
            case "Rutinario" -> 1;
            default -> 2;
        };
    }

    private UUID extractFirstRelation(NotionProperty prop) {
        if (prop == null || prop.getRelation() == null || prop.getRelation().isEmpty()) return null;
        try {
            // Nota: Este ID es el ID de Notion (hex). 
            // Para ser robustos, el Core debe usar este ID para buscar el mapeo de identidad.
            // Por ahora, devolvemos el UUID de Notion.
            return UUID.fromString(prop.getRelation().getFirst().getId());
        } catch (Exception e) {
            return null;
        }
    }

    private OffsetDateTime parseNotionDate(String dateStr) {
        if (dateStr == null) return null;
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

    private String mapDomainStatusToNotion(ExecutableStatus status) {
        return switch (status) {
            case IN_PROGRESS -> "In progress";
            case DONE -> "Done";
            default -> "Not started";
        };
    }
}
