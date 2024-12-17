-- =====================================================================================
-- Repeating group appointment with three appointments each with three attendees.
-- Appointment with id 1 starting yesterday has has no attendance marked.
-- Appointment with id 2 starting today has two attendees with attendance and non-attendance marked.
-- Appointment with id 3 starting tomorrow has has no attendance marked.
-- =====================================================================================

INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (1, 'DAILY', 3);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (1, 'GROUP', 'RSI', 'EDUC', 1, 123, false, now()::date - 1, '09:00', '10:30', 1, (now()::date - 2)::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (1, 1, 1, 'RSI', 'EDUC', 1, 123, false, now()::date - 1, '09:00', '10:30', now()::timestamp, 'TEST.USER'),
        (2, 1, 2, 'RSI', 'EDUC', 1, 123, false, now()::date, '09:00', '10:30', now()::timestamp, 'TEST.USER'),
        (3, 1, 3, 'RSI', 'EDUC', 1, 123, false, now()::date + 1, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (1, 1, 'A1234BC', 1),
        (2, 1, 'B2345CD', 2),
        (3, 1, 'C3456DE', 3);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, attended, attendance_recorded_time, attendance_recorded_by)
VALUES  (4, 2, 'A1234BC', 1, null, null, null),
        (5, 2, 'B2345CD', 2, true, (now()::date - 1)::timestamp, 'PREV.ATTENDANCE.RECORDED.BY'),
        (6, 2, 'C3456DE', 3, false, (now()::date - 1)::timestamp, 'PREV.ATTENDANCE.RECORDED.BY');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (7, 3, 'A1234BC', 1),
        (8, 3, 'B2345CD', 2),
        (9, 3, 'C3456DE', 3);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (2, 'GROUP', 'RSI', 'CANT', 1, 456, false, now()::date - 1, '09:00', '10:30', 1, (now()::date - 2)::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (4, 2, 1, 'RSI', 'CANT', 1, 456, false, now()::date, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (12, 4, 'Z3333ZZ', 6);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (3, 'GROUP', 'LPI', 'CANT', 1, 456, false, now()::date - 1, '09:00', '10:30', 1, (now()::date - 2)::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (5, 3, 1, 'LPI', 'CANT', 1, 456, false, now()::date, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (4, 'GROUP', 'RSI', 'CANT', 1, 123, false, now()::date - 1, '09:00', '10:30', 1, (now()::date - 2)::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (101, 4, 1, 'RSI', 'CANT', 1, 123, false, now()::date - 1, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (101, 101, 'XX1111X', 1),
        (102, 101, 'YY1111Y', 2);

