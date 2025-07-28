CREATE INDEX IF NOT EXISTS idx_attendance_case_note_id                ON attendance (case_note_id);
CREATE INDEX IF NOT EXISTS idx_attendance_history_case_note_id        ON attendance_history (case_note_id);
CREATE INDEX IF NOT EXISTS idx_allocation_case_note_id                ON allocation (deallocation_case_note_id);
CREATE INDEX IF NOT EXISTS idx_planned_suspension_case_note_id        ON planned_suspension (case_note_id);
CREATE INDEX IF NOT EXISTS idx_planned_deallocation_case_note_id      ON planned_deallocation (case_note_id);
