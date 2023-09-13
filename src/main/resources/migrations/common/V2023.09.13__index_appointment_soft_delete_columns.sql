CREATE INDEX idx_appointment_is_deleted ON appointment (is_deleted);
CREATE INDEX idx_appointment_attendee_removed_time ON appointment_attendee (removed_time);
