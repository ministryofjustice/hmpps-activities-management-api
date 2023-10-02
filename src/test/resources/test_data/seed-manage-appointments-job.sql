-- ===========================
-- REPEATING GROUP APPOINTMENT
-- ===========================

INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (1, 'DAILY', 1);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (1, 'GROUP', 'RSI', 'AC1', 4, 123, false, now()::date - 2, '09:00', '10:30', 1, now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (1, 1, 1, 'RSI', 'AC1', 4, 123, false, now()::date - 2, '09:00', '10:30', now()::timestamp, 'TEST.USER'),
        (2, 1, 2, 'RSI', 'AC1', 4, 123, false, now()::date - 1, '09:00', '10:30', now()::timestamp, 'TEST.USER'),
        (3, 1, 3, 'RSI', 'AC1', 4, 123, false, now()::date, '09:00', '10:30', now()::timestamp, 'TEST.USER'),
        (4, 1, 4, 'RSI', 'AC1', 4, 123, false, now()::date + 1, '09:00', '10:30', now()::timestamp, 'TEST.USER'),
        (5, 1, 5, 'RSI', 'AC1', 4, 123, false, now()::date + 2, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (1, 1, 'A1234BC', 123),
        (2, 1, 'B2345CD', 456),
        (3, 1, 'C3456DE', 769);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (4, 2, 'A1234BC', 123),
        (5, 2, 'B2345CD', 456),
        (6, 2, 'C3456DE', 769);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (7, 3, 'A1234BC', 123),
        (8, 3, 'B2345CD', 456),
        (9, 3, 'C3456DE', 769);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (10, 4, 'A1234BC', 123),
        (11, 4, 'B2345CD', 456),
        (12, 4, 'C3456DE', 769);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (13, 5, 'A1234BC', 123),
        (14, 5, 'B2345CD', 456),
        (15, 5, 'C3456DE', 769);

-- ===============
-- APPOINTMENT SET
-- ===============

INSERT INTO appointment_set (appointment_set_id, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, created_time, created_by)
VALUES  (1, 'RSI', 'AC1', 4, 123, false, now()::date, now()::timestamp, 'TEST.USER');

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (10, 'INDIVIDUAL', 'RSI', 'AC1', 4, 123, false, now()::date, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
        (11, 'INDIVIDUAL', 'RSI', 'AC1', 4, 123, false, now()::date, '09:15', '09:30', now()::timestamp, 'TEST.USER'),
        (12, 'INDIVIDUAL', 'RSI', 'AC1', 4, 123, false, now()::date, '09:30', '09:45', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (10, 10, 1, 'RSI', 'AC1', 4, 123, false, now()::date, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
        (11, 11, 1, 'RSI', 'AC1', 4, 123, false, now()::date, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
        (12, 12, 1, 'RSI', 'AC1', 4, 123, false, now()::date, '09:00', '09:15', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (16, 10, 'A1234BC', 123),
        (17, 11, 'B2345CD', 456),
        (18, 12, 'C3456DE', 769);

INSERT INTO appointment_set_appointment_series (appointment_set_appointment_series_id, appointment_set_id, appointment_series_id)
VALUES  (1, 1, 10),
        (2, 1, 11),
        (3, 1, 12);

