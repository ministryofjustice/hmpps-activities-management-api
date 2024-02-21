ALTER TABLE planned_suspension DROP COLUMN planned_reason;
ALTER TABLE planned_suspension ADD COLUMN case_note_id bigint;
