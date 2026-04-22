package com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity;

import com.hyperbrain.sopfc.core.domain.model.CoreExecutable.ExecutableType;
import com.hyperbrain.sopfc.core.domain.model.ExecutableStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "core_executable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreExecutableEntity {

    @Id
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "cycle_id")
    private UUID cycleId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExecutableType type = ExecutableType.TASK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExecutableStatus status = ExecutableStatus.PENDING;

    private String context;
    
    @Column(name = "priority_score")
    private Double priorityScore;

    @Column(name = "impact")
    private Integer impactScore;

    @OneToOne(mappedBy = "executable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CoreExecutionProfileEntity executionProfile;

    @Column(name = "is_planned")
    private boolean isPlanned;
    
    @Column(name = "start_time")
    private OffsetDateTime startTime;
    
    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "apple_priority")
    private Integer applePriority;

    @Column(name = "external_url")
    private String externalUrl;

    @Column(name = "completion_date")
    private OffsetDateTime completionDate;

    @Column(name = "last_modified_date")
    private OffsetDateTime lastModifiedDate;
}
