package com.hyperbrain.sopfc.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    @Column(name = "executable_id")
    private UUID executableId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    private Integer estimatedMinutes;
    private Integer mentalLoad;
    private Integer energyDrain;
}