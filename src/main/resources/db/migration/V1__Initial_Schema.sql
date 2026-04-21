-- Create schemas
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS sync;
CREATE SCHEMA IF NOT EXISTS common;

-- Create core tables
CREATE TABLE core.core_executable (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    context VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    priority_score DOUBLE PRECISION
);

CREATE TABLE core.core_execution_profile (
    executable_id UUID PRIMARY KEY REFERENCES core.core_executable(id),
    tenant_id UUID NOT NULL,
    estimated_minutes INTEGER,
    energy_drain INTEGER,
    mental_load INTEGER
);

-- Create Outbox table in common schema
CREATE TABLE common.outbox_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE
);

-- Enable RLS on all tables
ALTER TABLE core.core_executable ENABLE ROW LEVEL SECURITY;
ALTER TABLE core.core_execution_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE common.outbox_events ENABLE ROW LEVEL SECURITY;

-- Create RLS Policies
CREATE POLICY core_executable_isolation ON core.core_executable
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

CREATE POLICY core_execution_profile_isolation ON core.core_execution_profile
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

CREATE POLICY outbox_events_isolation ON common.outbox_events
    USING (tenant_id = current_setting('app.current_tenant')::UUID);
