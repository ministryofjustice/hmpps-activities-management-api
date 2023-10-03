-- ===========================
-- Non-repeating appointment for PVI CHAP increments:
-- - appointmentCount by 1,
-- - appointmentInstanceCount by 1,
-- - appointmentSeriesCount by 0,
-- - appointmentSetCount by 0,
-- - cancelledAppointmentCount by 0,
-- - deletedAppointmentCount by 0,
-- ===========================

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (1, 'GROUP', 'PVI', 'CHAP', 4, 123, false, now()::date - 2, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (1, 1, 1, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (1, 1, 'A1234BC', 123);

-- ===========================
-- Cancelled appointment for PVI CHAP increments:
-- - appointmentCount by 0,
-- - appointmentInstanceCount by 0,
-- - appointmentSeriesCount by 0,
-- - appointmentSetCount by 0,
-- - cancelledAppointmentCount by 1,
-- - deletedAppointmentCount by 0,
-- ===========================

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (2, 'GROUP', 'PVI', 'CHAP', 4, 123, false, now()::date - 2, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES  (2, 2, 1, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:00', '10:30', now()::timestamp, 'TEST.USER', now()::timestamp, 2, 'CANCEL.USER', false);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (2, 2, 'A1234BC', 123);

-- ===========================
-- Deleted appointment for PVI CHAP increments:
-- - appointmentCount by 0,
-- - appointmentInstanceCount by 0,
-- - appointmentSeriesCount by 0,
-- - appointmentSetCount by 0,
-- - cancelledAppointmentCount by 0,
-- - deletedAppointmentCount by 1,
-- ===========================

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (3, 'GROUP', 'PVI', 'CHAP', 4, 123, false, now()::date - 2, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES  (3, 3, 1, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:00', '10:30', now()::timestamp, 'TEST.USER', now()::timestamp, 1, 'DELETE.USER', true);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (3, 3, 'A1234BC', 123);

-- ===========================
-- Repeating group appointment with two appointments on same day for PVI CHAP increments:
-- - appointmentCount by 2,
-- - appointmentInstanceCount by 6,
-- - appointmentSeriesCount by 1,
-- - appointmentSetCount by 0,
-- - cancelledAppointmentCount by 0,
-- - deletedAppointmentCount by 0,
-- ===========================

INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (1, 'DAILY', 3);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (4, 'GROUP', 'PVI', 'CHAP', 4, 123, false, now()::date - 2, '09:00', '10:30', 1, now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (4, 4, 1, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:00', '10:30', now()::timestamp, 'TEST.USER'),
        (5, 4, 2, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '13:00', '14:30', now()::timestamp, 'TEST.USER'),
        (6, 4, 3, 'PVI', 'CHAP', 4, 123, false, now()::date + 1, '09:00', '10:30', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (4, 4, 'A1234BC', 123),
        (5, 4, 'B2345CD', 456),
        (6, 4, 'C3456DE', 769);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (7, 5, 'A1234BC', 123),
        (8, 5, 'B2345CD', 456),
        (9, 5, 'C3456DE', 769);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (10, 6, 'A1234BC', 123),
        (11, 6, 'B2345CD', 456),
        (12, 6, 'C3456DE', 769);

-- ===================================
-- Appointment set with four appointments, one cancelled, one deleted for PVI CHAP increments:
-- - appointmentCount by 2,
-- - appointmentInstanceCount by 2,
-- - appointmentSeriesCount by 0,
-- - appointmentSetCount by 1,
-- - cancelledAppointmentCount by 1,
-- - deletedAppointmentCount by 1,
-- ===================================

INSERT INTO appointment_set (appointment_set_id, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, created_time, created_by)
VALUES  (1, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, now()::timestamp, 'TEST.USER');

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (5, 'INDIVIDUAL', 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
        (6, 'INDIVIDUAL', 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:15', '09:30', now()::timestamp, 'TEST.USER'),
        (7, 'INDIVIDUAL', 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:30', '09:45', now()::timestamp, 'TEST.USER'),
        (8, 'INDIVIDUAL', 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:45', '10:00', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES  (7, 5, 1, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:00', '09:15', now()::timestamp, 'TEST.USER', null, null, null, false),
        (8, 6, 1, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:00', '09:15', now()::timestamp, 'TEST.USER', now()::timestamp, 2, 'CANCEL.USER', false),
        (9, 7, 1, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:00', '09:15', now()::timestamp, 'TEST.USER', null, null, null, false),
        (10, 8, 1, 'PVI', 'CHAP', 4, 123, false, now()::date - 1, '09:00', '09:15', now()::timestamp, 'TEST.USER', now()::timestamp, 1, 'DELETE.USER', true);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (13, 7, 'A1234BC', 123),
        (14, 8, 'B2345CD', 456),
        (15, 9, 'C3456DE', 769),
        (16, 10, 'D4567EF', 101112);

INSERT INTO appointment_set_appointment_series (appointment_set_appointment_series_id, appointment_set_id, appointment_series_id)
VALUES  (1, 1, 5),
        (2, 1, 6),
        (3, 1, 7),
        (4, 1, 8);
