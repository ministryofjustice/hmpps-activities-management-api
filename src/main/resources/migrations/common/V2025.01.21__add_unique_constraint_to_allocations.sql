ALTER TABLE allocation ADD CONSTRAINT unique_non_ended_prisoner_allocation_key
    EXCLUDE (activity_schedule_id WITH =, prisoner_number WITH =, prisoner_status WITH =)
    WHERE (prisoner_status != 'ENDED');