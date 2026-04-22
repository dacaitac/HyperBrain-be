ALTER TABLE common.outbox_events ADD COLUMN source_system VARCHAR(100);
UPDATE common.outbox_events SET source_system = 'SOPFC_INTERNAL' WHERE source_system IS NULL;
