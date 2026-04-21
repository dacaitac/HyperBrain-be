-- V3: Add SOPFC specific fields to Core Executable
ALTER TABLE core.core_executable ADD COLUMN impact INTEGER;
ALTER TABLE core.core_executable ADD COLUMN is_planned BOOLEAN DEFAULT FALSE;
ALTER TABLE core.core_executable ADD COLUMN start_time TIMESTAMP WITH TIME ZONE;
ALTER TABLE core.core_executable ADD COLUMN end_time TIMESTAMP WITH TIME ZONE;

-- Update RLS policies if needed (they are already set to tenant_id, so no changes needed for isolation)
