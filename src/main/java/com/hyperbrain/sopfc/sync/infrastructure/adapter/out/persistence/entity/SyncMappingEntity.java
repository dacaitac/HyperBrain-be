package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_mappings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "executable_id", nullable = false)
    private UUID executableId;

    @Column(name = "external_system", nullable = false, length = 50)
    private String externalSystem;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "last_known_checksum", length = 255)
    private String lastKnownChecksum;

    @Column(name = "last_synced_at")
    @Builder.Default
    private OffsetDateTime lastSyncedAt = OffsetDateTime.now();

    @Column(name = "sync_status", length = 20)
    @ColumnDefault("'IN_SYNC'")
    private String syncStatus;
}
