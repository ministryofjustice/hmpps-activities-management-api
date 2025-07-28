-- Update the duplicate case notes ids with correct ids
UPDATE public.attendance SET case_note_id = 102321962
WHERE case_note_id = 102321963
UPDATE public.attendance SET case_note_id = 103048197
WHERE case_note_id = 103048198
UPDATE public.attendance SET case_note_id = 104577477
WHERE case_note_id = 104577479
UPDATE public.attendance SET case_note_id = 104794436
WHERE case_note_id = 104794440
UPDATE public.attendance SET case_note_id = 104794443
WHERE case_note_id = 104794482
UPDATE public.attendance_history SET case_note_id = 103425760
WHERE case_note_id = 103425759

CREATE INDEX IF NOT EXISTS idx_attendance_case_note_id                ON attendance (case_note_id);
CREATE INDEX IF NOT EXISTS idx_attendance_history_case_note_id        ON attendance_history (case_note_id);
CREATE INDEX IF NOT EXISTS idx_allocation_case_note_id                ON allocation (deallocation_case_note_id);
CREATE INDEX IF NOT EXISTS idx_planned_suspension_case_note_id        ON planned_suspension (case_note_id);
CREATE INDEX IF NOT EXISTS idx_planned_deallocation_case_note_id      ON planned_deallocation (case_note_id);
