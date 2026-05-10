package com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "core_execution_profile", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreExecutionProfileEntity {

    @Id
    private UUID executable_id;

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
