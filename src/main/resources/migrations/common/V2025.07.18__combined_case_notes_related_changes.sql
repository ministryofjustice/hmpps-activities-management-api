ALTER TABLE attendance              ADD COLUMN IF NOT EXISTS dps_case_note_id              VARCHAR(36);
ALTER TABLE attendance_history      ADD COLUMN IF NOT EXISTS dps_case_note_id              VARCHAR(36);
ALTER TABLE planned_suspension      ADD COLUMN IF NOT EXISTS dps_case_note_id              VARCHAR(36);
ALTER TABLE planned_deallocation    ADD COLUMN IF NOT EXISTS dps_case_note_id              VARCHAR(36);
ALTER TABLE allocation              ADD COLUMN IF NOT EXISTS deallocation_dps_case_note_id VARCHAR(36);
