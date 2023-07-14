ALTER TABLE activity_schedule ADD COLUMN schedule_weeks integer NOT NULL DEFAULT 1;

ALTER TABLE activity_schedule_slot ADD COLUMN week_number integer NOT NULL DEFAULT 1;
