ALTER TABLE allocation ADD COLUMN IF NOT EXISTS deallocation_case_note_id bigint;
ALTER TABLE planned_deallocation ADD COLUMN IF NOT EXISTS case_note_id bigint;
