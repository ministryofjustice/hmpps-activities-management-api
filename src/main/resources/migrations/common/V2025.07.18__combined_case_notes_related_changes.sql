ALTER TABLE attendance              ADD COLUMN IF NOT EXISTS dps_case_note_id              UUID;
ALTER TABLE attendance_history      ADD COLUMN IF NOT EXISTS dps_case_note_id              UUID;
ALTER TABLE allocation              ADD COLUMN IF NOT EXISTS deallocation_dps_case_note_id UUID;
ALTER TABLE planned_suspension      ADD COLUMN IF NOT EXISTS dps_case_note_id              UUID;
ALTER TABLE planned_deallocation    ADD COLUMN IF NOT EXISTS dps_case_note_id              UUID;

CREATE INDEX idx_attendance_case_note_id                ON attendance (case_note_id);
CREATE INDEX idx_attendance_history_case_note_id        ON attendance_history (case_note_id);
CREATE INDEX idx_allocation_case_note_id                ON allocation (deallocation_case_note_id);
CREATE INDEX idx_planned_suspension_case_note_id        ON planned_suspension (case_note_id);
CREATE INDEX idx_planned_deallocation_case_note_id      ON planned_deallocation (case_note_id);
