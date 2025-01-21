ALTER TABLE allocation ADD UNIQUE (activity_schedule_id, prisoner_number, booking_id, allocated_time, prisoner_status);
