-- Create core tables
CREATE TABLE IF NOT EXISTS core.core_executable (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    context VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    priority_score DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS core.core_execution_profile (
    executable_id UUID PRIMARY KEY REFERENCES core.core_executable(id),
    estimated_minutes INTEGER,
    energy_drain INTEGER,
    mental_load INTEGER
);

-- Create Outbox table in common schema
CREATE TABLE IF NOT EXISTS common.outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE
);
