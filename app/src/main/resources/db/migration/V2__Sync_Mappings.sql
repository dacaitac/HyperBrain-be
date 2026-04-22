-- V2: Sync Engine and External Integrations Schema
CREATE TABLE IF NOT EXISTS sync.sync_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    executable_id UUID NOT NULL REFERENCES core.core_executable(id) ON DELETE CASCADE,
    external_system VARCHAR(50) NOT NULL, -- 'NOTION', 'APPLE_REMINDERS', 'APPLE_CALENDAR'
    external_id VARCHAR(255) NOT NULL,
    last_known_checksum VARCHAR(255),
    last_synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sync_status VARCHAR(20) DEFAULT 'IN_SYNC',
    
    UNIQUE(external_system, external_id),
    UNIQUE(executable_id, external_system)
);

-- Indexes for performance
CREATE INDEX idx_sync_mappings_executable ON sync.sync_mappings(executable_id);
CREATE INDEX idx_sync_mappings_external ON sync.sync_mappings(external_system, external_id);
