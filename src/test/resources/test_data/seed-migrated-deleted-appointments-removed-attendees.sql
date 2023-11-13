INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, start_date, start_time, end_time, created_time, created_by, is_migrated)
VALUES
    (1, 'INDIVIDUAL', 'RSI', 'CHAP', 1, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', true),
    (2, 'INDIVIDUAL', 'RSI', 'CHAP', 1, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', true),
    (3, 'INDIVIDUAL', 'RSI', 'CHAP', 1, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', true),
    (4, 'INDIVIDUAL', 'RSI', 'CHAP', 1, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', true);

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES
    (10, 1, 1, 'RSI', 'CHAP', 1, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', now()::timestamp, 2, 'CANCEL.USER', false), -- Cancelled appointment
    (11, 2, 1, 'RSI', 'CHAP', 1, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', now()::timestamp, 1, 'DELETE.USER', true), -- Deleted appointment
    (12, 3, 1, 'RSI', 'CHAP', 1, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', null, null, null, false), -- Appointment with single temporarily removed attendee
    (13, 4, 1, 'RSI', 'CHAP', 1, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', null, null, null, false); -- Appointment with single permanently removed attendee

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES
    (20, 10, 'A1234BC', 1, null, null, null, false),
    (21, 11, 'B2345CD', 2, null, null, null, false),
    (22, 12, 'C3456DE', 3, now()::timestamp, 2, 'REMOVED.BY', false), -- Temporary removal by user
    (23, 13, 'D4567EF', 4, now()::timestamp, 4, 'MANAGE_APPOINTMENT_SERVICE', true); -- Prisoner status: Released
