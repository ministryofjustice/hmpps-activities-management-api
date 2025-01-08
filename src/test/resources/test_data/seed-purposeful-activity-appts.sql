-- =====================================================================================
-- Repeating group appointment with three appointments each with three attendees.
-- Appointment with id 1 starting yesterday has no attendance marked.
-- Appointment with id 2 starting today has two attendees with attendance and non-attendance marked.
-- Appointment with id 3 starting tomorrow has no attendance marked.
-- =====================================================================================
DROP TABLE IF EXISTS temp_earliest_date;
-- Calculate earliest_date separately
CREATE TEMP TABLE temp_earliest_date AS
SELECT (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + 1 * 7 + 7))::timestamp AS earliest_date;

-- Use the earliest_date in subsequent INSERTs
INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (1, 'DAILY', 3);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (1, 'GROUP', 'RSI', 'EDUC', 1, 123, false, (SELECT earliest_date FROM temp_earliest_date) + INTERVAL '1 day', '09:00', '10:30', 1, (now()::date - 2)::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (1, 1, 1, 'RSI', 'EDUC', 1, 123, false, (SELECT earliest_date FROM temp_earliest_date) + INTERVAL '1 day', '09:00', '10:30', now()::timestamp, 'TEST.USER'),
        (2, 1, 2, 'RSI', 'EDUC', 1, 123, false, (SELECT earliest_date FROM temp_earliest_date) + INTERVAL '2 day', '09:00', '10:30', now()::timestamp, 'TEST.USER'),
        (3, 1, 3, 'RSI', 'EDUC', 1, 123, false, (SELECT earliest_date FROM temp_earliest_date) + INTERVAL '9 day', '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (1, 1, 'A1234BC', 1),
        (2, 1, 'B2345CD', 2),
        (3, 1, 'C3456DE', 3);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, attended, attendance_recorded_time, attendance_recorded_by)
VALUES  (4, 2, 'A1234BC', 1, null, null, null),
        (5, 2, 'B2345CD', 2, true, (SELECT earliest_date FROM temp_earliest_date) + INTERVAL '1 day', 'PREV.ATTENDANCE.RECORDED.BY'),
        (6, 2, 'C3456DE', 3, false, (SELECT earliest_date FROM temp_earliest_date) + INTERVAL '1 day', 'PREV.ATTENDANCE.RECORDED.BY');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (7, 3, 'A1234BC', 1),
        (8, 3, 'B2345CD', 2),
        (9, 3, 'C3456DE', 3);