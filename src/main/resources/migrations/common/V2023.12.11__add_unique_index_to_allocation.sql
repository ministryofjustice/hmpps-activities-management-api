--
-- Adding constraint to prevent duplicate allocations to the same activity.  Note including the prisoner status should be safe due the fact allocations are future dated.
--
CREATE UNIQUE INDEX idx_schedule_id_prisoner_number_start_date_prisoner_status ON allocation (activity_schedule_id, prisoner_number, start_date, prisoner_status);
