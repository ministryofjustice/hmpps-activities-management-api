ALTER TABLE attendance              ADD COLUMN IF NOT EXISTS dps_case_note_id              UUID;
ALTER TABLE attendance_history      ADD COLUMN IF NOT EXISTS dps_case_note_id              UUID;
ALTER TABLE allocation              ADD COLUMN IF NOT EXISTS deallocation_dps_case_note_id UUID;
ALTER TABLE planned_suspension      ADD COLUMN IF NOT EXISTS dps_case_note_id              UUID;
ALTER TABLE planned_deallocation    ADD COLUMN IF NOT EXISTS dps_case_note_id              UUID;
