-- ==================================================================
-- REPEATING GROUP APPOINTMENT
-- Contains:
-- - Appointment id = 1 starting yesterday
-- - Appointment id = 2 starting today with only one attendee, B2345CD
-- - Appointment id = 3 starting tomorrow
-- - Temporarily removed attendee appointment id = 4, attendee id = 5
-- - Permanently removed attendee appointment id = 5, attendee id = 7
-- - Cancelled appointment id = 6
-- - Deleted appointment id = 7
-- ==================================================================

INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (1, 'DAILY', 7);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (1, 'GROUP', 'RSI', 'CHAP', 1, 123, false, now()::date - 1, '09:00', '10:30', 1, now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES  (1, 1, 1, 'RSI', 'CHAP', 1, 123, false, now()::date - 1, '09:00', '10:30', now()::timestamp, 'TEST.USER', null, null, null, false),
        (2, 1, 2, 'RSI', 'CHAP', 1, 123, false, now()::date, '09:00', '10:30', now()::timestamp, 'TEST.USER', null, null, null, false),
        (3, 1, 2, 'RSI', 'CHAP', 1, 123, false, now()::date + 1, '09:00', '10:30', now()::timestamp, 'TEST.USER', null, null, null, false),
        (4, 1, 3, 'RSI', 'CHAP', 1, 123, false, now()::date + 2, '09:00', '10:30', now()::timestamp, 'TEST.USER', null, null, null, false),
        (5, 1, 4, 'RSI', 'CHAP', 1, 123, false, now()::date + 3, '09:00', '10:30', now()::timestamp, 'TEST.USER', null, null, null, false),
        (6, 1, 5, 'RSI', 'CHAP', 1, 123, false, now()::date + 4, '09:00', '10:30', now()::timestamp, 'TEST.USER', now()::timestamp, 2, 'CANCEL.USER', false), -- Cancelled appointment
        (7, 1, 6, 'RSI', 'CHAP', 1, 123, false, now()::date + 5, '09:00', '10:30', now()::timestamp, 'TEST.USER', now()::timestamp, 1, 'DELETE.USER', true); -- Deleted appointment

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (1, 1, 'A1234BC', 1),
        (2, 1, 'B2345CD', 2);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (3, 2, 'B2345CD', 2);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (4, 3, 'A1234BC', 1),
        (5, 3, 'B2345CD', 2);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES  (6, 4, 'A1234BC', 1, now()::timestamp, 2, 'REMOVED.BY', false), -- Temporary removal by user
        (7, 4, 'B2345CD', 2, null, null, null, false);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES  (8, 5, 'A1234BC', 1, now()::timestamp, 1, 'REMOVED.BY', true), -- Permanent removal by user
        (9, 5, 'B2345CD', 2, null, null, null, false);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (10, 6, 'A1234BC', 1),
        (11, 6, 'B2345CD', 2);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (12, 7, 'A1234BC', 1),
        (13, 7, 'B2345CD', 2);

-- ====================================
-- APPOINTMENT SET STARTING IN TWO DAYS
-- ====================================

INSERT INTO appointment_set (appointment_set_id, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, created_time, created_by)
VALUES  (1, 'RSI', 'AC1', 1, 123, false, now()::date + 2, now()::timestamp, 'TEST.USER');

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (10, 'INDIVIDUAL', 'RSI', 'AC1', 1, 123, false, now()::date + 2, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
        (11, 'INDIVIDUAL', 'RSI', 'AC1', 1, 123, false, now()::date + 2, '09:15', '09:30', now()::timestamp, 'TEST.USER'),
        (12, 'INDIVIDUAL', 'RSI', 'AC1', 1, 123, false, now()::date + 2, '09:30', '09:45', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (10, 10, 1, 'RSI', 'AC1', 1, 123, false, now()::date + 2, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
        (11, 11, 1, 'RSI', 'AC1', 1, 123, false, now()::date + 2, '09:15', '09:30', now()::timestamp, 'TEST.USER'),
        (12, 12, 1, 'RSI', 'AC1', 1, 123, false, now()::date + 2, '09:30', '09:45', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (20, 10, 'A1234BC', 123),
        (21, 11, 'B2345CD', 456),
        (22, 12, 'C3456DE', 769);

INSERT INTO appointment_set_appointment_series (appointment_set_appointment_series_id, appointment_set_id, appointment_series_id)
VALUES  (1, 1, 10),
        (2, 1, 11),
        (3, 1, 12);

