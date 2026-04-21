package com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "core_execution_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreExecutionProfileEntity {

    @Id
    private UUID executable_id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "executable_id")
    private CoreExecutableEntity executable;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "energy_drain")
    private Integer energyDrain;

    @Column(name = "mental_load")
    private Integer mentalLoad;
}
