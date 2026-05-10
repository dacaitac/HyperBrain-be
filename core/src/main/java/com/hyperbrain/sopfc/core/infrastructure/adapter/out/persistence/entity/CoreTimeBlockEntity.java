package com.hyperbrain.sopfc.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "core_time_block", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreTimeBlockEntity {
    @Id
    private UUID id;

    @Column(name = "executable_id", nullable = false)
    private UUID executableId;

    @Column(name = "date_start", nullable = false)
    private OffsetDateTime dateStart;

    @Column(name = "date_end", nullable = false)
    private OffsetDateTime dateEnd;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;
}
