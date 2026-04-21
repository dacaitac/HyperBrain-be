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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "cycle_id")
    private UUID cycleId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutableType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutableStatus status;

    private String context;
    
    @Column(name = "priority_score")
    private Double priorityScore;

    // Perfil de ejecución (Mapeado desde la tabla core_execution_profile)
    @OneToOne(mappedBy = "executable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CoreExecutionProfileEntity executionProfile;

    @Column(name = "is_planned")
    private boolean isPlanned;
    
    @Column(name = "start_time")
    private OffsetDateTime startTime;
    
    @Column(name = "end_time")
    private OffsetDateTime endTime;
}
