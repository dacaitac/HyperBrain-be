package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.core.domain.model.CoreExecutable.ExecutableType;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import com.hyperbrain.sopfc.sync.application.util.SyncUtils;
import com.hyperbrain.sopfc.sync.domain.port.out.ExternalSyncPort;
import com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto.*;
import com.hyperbrain.sopfc.sync.infrastructure.config.notion.NotionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto.NotionConstants.*;

@Slf4j
@Component
public class NotionSyncAdapter implements ExternalSyncPort {

    private final WebClient webClient;
    private final NotionProperties notionProperties;
    private final String tasksDbId;
    private final ObjectMapper objectMapper;
    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    public NotionSyncAdapter(WebClient.Builder webClientBuilder, NotionProperties notionProperties, ObjectMapper objectMapper) {
        this.notionProperties = notionProperties;
        this.objectMapper = objectMapper;
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
        String normalizedId = SyncUtils.normalizeNotionId(externalId);
        log.info("🔍 [NOTION-FETCH] Fetching page: {}", normalizedId);
        try {
            return webClient.get()
                    .uri("/pages/{pageId}", normalizedId)
                    .retrieve()
                    .bodyToMono(NotionPageResponse.class)
                    .filter(response -> !response.isArchived())
                    .map(response -> new ExternalSyncResult(toDomain(response), SyncUtils.normalizeNotionId(response.getId())))
                    .blockOptional();
        } catch (Exception e) {
            log.warn("⚠️ [NOTION-FETCH] Error fetching page {}: {}. Assuming archived/deleted.", normalizedId, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isArchived(String externalId) {
        String normalizedId = SyncUtils.normalizeNotionId(externalId);
        try {
            return Boolean.TRUE.equals(webClient.get()
                    .uri("/pages/{pageId}", normalizedId)
                    .retrieve()
                    .bodyToMono(NotionPageResponse.class)
                    .map(NotionPageResponse::isArchived)
                    .onErrorReturn(true)
                    .block());
        } catch (Exception e) {
            log.warn("⚠️ [NOTION-FETCH] Error checking status for {}: {}. Assuming archived/deleted.", normalizedId, e.getMessage());
            return true;
        }
    }

    @Override
    public List<ExternalSyncResult> fetchDelta() {
        Map<String, Object> queryBody = Map.of(
            "sorts", List.of(Map.of("timestamp", "last_edited_time", "direction", "descending"))
        );

        return webClient.post()
                .uri("/databases/{databaseId}/query", tasksDbId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(queryBody)
                .retrieve()
                .bodyToMono(NotionQueryResponse.class)
                .map(response -> response.getResults().stream()
                        .filter(res -> !res.isArchived())
                        .map(res -> new ExternalSyncResult(toDomain(res), SyncUtils.normalizeNotionId(res.getId())))
                        .collect(Collectors.toList()))
                .block();
    }

    @Override
    public void pushUpdate(CoreExecutable executable, String externalId) {
        String normalizedId = SyncUtils.normalizeNotionId(externalId);
        Map<String, NotionProperty> properties = toNotionProperties(executable);
        webClient.patch()
                .uri("/pages/{pageId}", normalizedId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("properties", properties))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public String pushCreate(CoreExecutable executable) {
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
                .block();

        return response != null ? SyncUtils.normalizeNotionId(response.getId()) : null;
    }

    @Override
    public void pushDelete(String externalId) {
        String normalizedId = SyncUtils.normalizeNotionId(externalId);
        webClient.patch()
                .uri("/pages/{pageId}", normalizedId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("archived", true))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public String getSystemIdentifier() {
        return "NOTION";
    }

    public CoreExecutable toDomain(NotionPageResponse res) {
        Map<String, NotionProperty> props = res.getProperties();
        
        String name = Optional.ofNullable(props.get(PROP_NAME))
                .map(p -> p.getTitle() != null && !p.getTitle().isEmpty() ? p.getTitle().get(0).getText().getContent() : "")
                .orElse("");

        String description = Optional.ofNullable(props.get(PROP_DESCRIPTION))
                .map(p -> p.getRichText() != null && !p.getRichText().isEmpty() ? p.getRichText().get(0).getText().getContent() : "")
                .orElse("");

        ExecutableType type = mapNotionType(props.get(PROP_TYPE));
        
        // PRIORITY: Use the "Complete" checkbox first as it's easier to toggle.
        ExecutableStatus status;
        NotionProperty completeProp = props.get(PROP_COMPLETE);
        if (completeProp != null && completeProp.getCheckbox() != null) {
            status = completeProp.getCheckbox() ? ExecutableStatus.DONE : ExecutableStatus.PENDING;
            log.info("🎯 [NOTION-MAP] Status derived from 'Complete' checkbox: {}", status);
        } else {
            status = mapNotionStatus(props.get(PROP_STATUS));
        }

        Integer impact = mapSelectToImpact(props.get(PROP_IMPACT));
        Integer energy = mapSelectToEnergy(props.get(PROP_ENERGY));
        Integer mental = mapSelectToMental(props.get(PROP_MENTAL_LOAD));
        
        String url = Optional.ofNullable(props.get(PROP_URL)).map(NotionProperty::getUrl).orElse(null);

        Integer estimatedMinutes = Optional.ofNullable(props.get(PROP_EST_MIN))
                .map(p -> p.getNumber() != null ? p.getNumber().intValue() : 0)
                .orElse(0);

        OffsetDateTime start = null;
        OffsetDateTime end = null;
        NotionProperty dateProp = props.get(PROP_DATE);
        if (dateProp != null && dateProp.getDate() != null) {
            start = parseNotionDate(dateProp.getDate().getStart());
            end = parseNotionDate(dateProp.getDate().getEnd());
        }

        // Mapping Apple Priority back if it was set via "Important"
        Integer applePriority = null;
        NotionProperty importantProp = props.get(PROP_IMPORTANT);
        if (importantProp != null && Boolean.TRUE.equals(importantProp.getCheckbox())) {
            applePriority = 1; // High priority in Apple
        }

        return CoreExecutable.builder()
                .name(name)
                .description(description)
                .type(type)
                .status(status)
                .impactScore(impact)
                .energyDrain(energy)
                .mentalLoad(mental)
                .estimatedMinutes(estimatedMinutes)
                .externalUrl(url)
                .startTime(start)
                .endTime(end)
                .isPlanned(start != null)
                .applePriority(applePriority)
                .build();
    }

    private Integer mapSelectToEnergy(NotionProperty prop) {
        if (prop == null || prop.getSelect() == null) return 3;
        return switch (prop.getSelect().getName()) {
            case ENERGY_INTENSE -> 5;
            case ENERGY_DEMANDING -> 4;
            case ENERGY_SUSTAINED -> 3;
            case ENERGY_EXECUTION -> 2;
            case ENERGY_AUTOMATIC -> 1;
            default -> 3;
        };
    }

    private String mapDomainEnergyToNotion(Integer energy) {
        if (energy == null) return ENERGY_SUSTAINED;
        return switch (energy) {
            case 5 -> ENERGY_INTENSE;
            case 4 -> ENERGY_DEMANDING;
            case 3 -> ENERGY_SUSTAINED;
            case 2 -> ENERGY_EXECUTION;
            case 1 -> ENERGY_AUTOMATIC;
            default -> ENERGY_SUSTAINED;
        };
    }

    private ExecutableType mapNotionType(NotionProperty prop) {
        if (prop == null || prop.getSelect() == null) return ExecutableType.TASK;
        return switch (prop.getSelect().getName()) {
            case TYPE_HABIT -> ExecutableType.HABIT;
            case TYPE_ACTIVITY -> ExecutableType.ACTIVITY;
            default -> ExecutableType.TASK;
        };
    }

    private String mapDomainTypeToNotion(ExecutableType type) {
        if (type == null) return TYPE_TASK;
        return switch (type) {
            case HABIT -> TYPE_HABIT;
            case ACTIVITY -> TYPE_ACTIVITY;
            default -> TYPE_TASK;
        };
    }

    private ExecutableStatus mapNotionStatus(NotionProperty prop) {
        if (prop == null || prop.getStatus() == null) return ExecutableStatus.PENDING;
        return switch (prop.getStatus().getName()) {
            case STATUS_IN_PROGRESS -> ExecutableStatus.IN_PROGRESS;
            case STATUS_DONE -> ExecutableStatus.DONE;
            case STATUS_FAILED -> ExecutableStatus.FAILED;
            default -> ExecutableStatus.PENDING;
        };
    }

    private String mapDomainStatusToNotion(ExecutableStatus status) {
        if (status == null) return STATUS_NOT_STARTED;
        return switch (status) {
            case IN_PROGRESS -> STATUS_IN_PROGRESS;
            case DONE -> STATUS_DONE;
            case FAILED -> STATUS_FAILED;
            default -> STATUS_NOT_STARTED;
        };
    }

    private Integer mapSelectToImpact(NotionProperty prop) {
        if (prop == null || prop.getSelect() == null) return 3;
        return switch (prop.getSelect().getName()) {
            case IMPACT_CRITICAL -> 5;
            case IMPACT_HIGH -> 4;
            case IMPACT_MODERATE -> 3;
            case IMPACT_LOW -> 2;
            case IMPACT_IRRELEVANT -> 1;
            default -> 3;
        };
    }

    private String mapDomainImpactToNotion(Integer impact) {
        if (impact == null) return IMPACT_MODERATE;
        return switch (impact) {
            case 5 -> IMPACT_CRITICAL;
            case 4 -> IMPACT_HIGH;
            case 3 -> IMPACT_MODERATE;
            case 2 -> IMPACT_LOW;
            case 1 -> IMPACT_IRRELEVANT;
            default -> IMPACT_MODERATE;
        };
    }

    private Integer mapSelectToMental(NotionProperty prop) {
        if (prop == null || prop.getSelect() == null) return 2;
        return switch (prop.getSelect().getName()) {
            case MENTAL_ABSTRACT -> 5;
            case MENTAL_COMPLEX -> 4;
            case MENTAL_ANALYSIS -> 3;
            case MENTAL_FOCUS -> 2;
            case MENTAL_ROUTINE -> 1;
            default -> 2;
        };
    }

    private String mapDomainMentalToNotion(Integer mental) {
        if (mental == null) return MENTAL_FOCUS;
        return switch (mental) {
            case 5 -> MENTAL_ABSTRACT;
            case 4 -> MENTAL_COMPLEX;
            case 3 -> MENTAL_ANALYSIS;
            case 2 -> MENTAL_FOCUS;
            case 1 -> MENTAL_ROUTINE;
            default -> MENTAL_FOCUS;
        };
    }

    private OffsetDateTime parseNotionDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            if (dateStr.length() == 10) {
                // For date-only strings, assume Bogota start of day
                return java.time.LocalDate.parse(dateStr).atStartOfDay(BOGOTA_ZONE).toOffsetDateTime();
            }
            return OffsetDateTime.parse(dateStr).atZoneSameInstant(BOGOTA_ZONE).toOffsetDateTime();
        } catch (Exception e) { return null; }
    }

    private Map<String, NotionProperty> toNotionProperties(CoreExecutable executable) {
        Map<String, NotionProperty> props = new java.util.HashMap<>();
        props.put(PROP_NAME, NotionProperty.title(executable.getName()));
        props.put(PROP_STATUS, NotionProperty.status(mapDomainStatusToNotion(executable.getStatus())));
        props.put(PROP_COMPLETE, NotionProperty.checkbox(executable.getStatus() == ExecutableStatus.DONE));
        props.put(PROP_TYPE, NotionProperty.select(mapDomainTypeToNotion(executable.getType())));

        // Priority Mapping: High priority (1-4) in Apple marks "Important" in Notion
        if (executable.getApplePriority() != null && executable.getApplePriority() > 0 && executable.getApplePriority() <= 4) {
            props.put(PROP_IMPORTANT, NotionProperty.checkbox(true));
        } else {
            props.put(PROP_IMPORTANT, NotionProperty.checkbox(false));
        }

        if (executable.getDescription() != null && !executable.getDescription().isBlank()) {
            props.put(PROP_DESCRIPTION, NotionProperty.richText(executable.getDescription()));
        }
        if (executable.getImpactScore() != null) {
            props.put(PROP_IMPACT, NotionProperty.select(mapDomainImpactToNotion(executable.getImpactScore())));
        }
        if (executable.getEnergyDrain() != null) {
            props.put(PROP_ENERGY, NotionProperty.select(mapDomainEnergyToNotion(executable.getEnergyDrain())));
        }
        if (executable.getMentalLoad() != null) {
            props.put(PROP_MENTAL_LOAD, NotionProperty.select(mapDomainMentalToNotion(executable.getMentalLoad())));
        }
        if (executable.getExternalUrl() != null) {
            props.put(PROP_URL, NotionProperty.builder().url(executable.getExternalUrl()).build());
        }
        if (executable.getEstimatedMinutes() != null) {
            props.put(PROP_EST_MIN, NotionProperty.number(executable.getEstimatedMinutes().doubleValue()));
        }
        if (executable.getStartTime() != null) {
            // Convert to Bogota timezone before sending to Notion
            OffsetDateTime bogotaStart = executable.getStartTime().atZoneSameInstant(BOGOTA_ZONE).toOffsetDateTime();
            
            String start;
            if (bogotaStart.getHour() == 0 && bogotaStart.getMinute() == 0 && bogotaStart.getSecond() == 0) {
                start = bogotaStart.toLocalDate().toString(); // YYYY-MM-DD
            } else {
                start = bogotaStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
            
            String end = null;
            if (executable.getEndTime() != null) {
                OffsetDateTime bogotaEnd = executable.getEndTime().atZoneSameInstant(BOGOTA_ZONE).toOffsetDateTime();
                if (bogotaEnd.getHour() == 0 && bogotaEnd.getMinute() == 0 && bogotaEnd.getSecond() == 0) {
                    end = bogotaEnd.toLocalDate().toString();
                } else {
                    end = bogotaEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            }
            props.put(PROP_DATE, NotionProperty.date(start, end));
        }
        return props;
    }
}
