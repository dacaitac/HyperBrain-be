package com.hyperbrain.sopfc.infrastructure.adapter.out.persistence.entity;

import com.hyperbrain.sopfc.domain.model.ExecutableStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

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

    private String name;

    @Enumerated(EnumType.STRING)
    private ExecutableStatus status;

    private String context;
    private Double priorityScore;
    
    private Integer impact;
    
    @Column(name = "is_planned")
    private boolean isPlanned;
    
    @Column(name = "start_time")
    private java.time.OffsetDateTime startTime;
    
    @Column(name = "end_time")
    private java.time.OffsetDateTime endTime;

    @OneToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private CoreExecutionProfileEntity executionProfile;
}