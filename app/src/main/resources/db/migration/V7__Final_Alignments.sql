-- V7: Final Alignments and Missing Tables

-- 1. Tables for CORE schema
CREATE TABLE IF NOT EXISTS core.core_project (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    total_invested_value NUMERIC(19, 4),
    status VARCHAR(50),
    version INTEGER
);

CREATE TABLE IF NOT EXISTS core.core_time_block (
    id UUID PRIMARY KEY,
    executable_id UUID NOT NULL REFERENCES core.core_executable(id),
    date_start TIMESTAMP WITH TIME ZONE NOT NULL,
    date_end TIMESTAMP WITH TIME ZONE NOT NULL,
    actual_duration_minutes INTEGER
);

-- 2. Tables for COGNITIVE schema
CREATE TABLE IF NOT EXISTS cognitive.cognitive_load (
    id UUID PRIMARY KEY,
    score INTEGER,
    source VARCHAR(255)
);

-- 3. Tables for FINANCE schema (Aligning with entities if needed)
-- Note: V4 already created finance.fin_transaction. 
-- If FinanceTransactionEntity is used, it expects finance_transactions.
CREATE TABLE IF NOT EXISTS finance.finance_transactions (
    id UUID PRIMARY KEY,
    description VARCHAR(500),
    amount NUMERIC(19, 4),
    category VARCHAR(255)
);

-- 4. Missing fields in core_executable
ALTER TABLE core.core_executable ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
ALTER TABLE core.core_executable ADD COLUMN IF NOT EXISTS apple_priority INTEGER;
ALTER TABLE core.core_executable ADD COLUMN IF NOT EXISTS external_url VARCHAR(1000);
ALTER TABLE core.core_executable ADD COLUMN IF NOT EXISTS completion_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE core.core_executable ADD COLUMN IF NOT EXISTS last_modified_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE core.core_executable ADD COLUMN IF NOT EXISTS source_calendar VARCHAR(255);
ALTER TABLE core.core_executable ADD COLUMN IF NOT EXISTS alarms JSONB;
ALTER TABLE core.core_executable ADD COLUMN IF NOT EXISTS recurrence VARCHAR(255);
