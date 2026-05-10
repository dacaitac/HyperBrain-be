package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_mappings", schema = "sync")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncMappingEntity {

    @Id
    private UUID id;

    @Column(name = "executable_id", nullable = false)
    private UUID executableId;

    @Column(name = "external_system", length = 50, nullable = false)
    private String externalSystem;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "last_known_checksum")
    private String lastKnownChecksum;

    @Column(name = "last_synced_at")
    @Builder.Default
    private OffsetDateTime lastSyncedAt = OffsetDateTime.now();

    @Column(name = "sync_status", length = 20)
    @ColumnDefault("'IN_SYNC'")
    private String syncStatus;
}
