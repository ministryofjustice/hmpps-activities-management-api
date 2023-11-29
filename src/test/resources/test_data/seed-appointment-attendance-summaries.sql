-- =====================================================================================
-- Repeating group appointment with three appointments each with three attendees.
-- Appointment with id 1 starting yesterday has has no attendance marked and was cancelled.
-- Appointment with id 2 starting today has six attendees with three attended and two non-attended.
-- Appointment with id 3 starting tomorrow has removed and soft deleted attendees with attendance records.
-- =====================================================================================

INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (1, 'DAILY', 3);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (1, 'GROUP', 'RSI', 'EDUC', 1, 123, false, now()::date - 1, '09:00', '10:30', 1, (now()::date - 2)::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES  (1, 1, 1, 'RSI', 'EDUC', 1, 123, false, now()::date - 1, '09:00', '10:30', (now()::date - 2)::timestamp, 'TEST.USER', (now()::date - 1)::timestamp, 2, 'CANCEL.USER', false),
        (2, 1, 2, 'RSI', 'EDUC', 1, 123, false, now()::date, '09:00', '10:30', (now()::date - 2)::timestamp, 'TEST.USER', null, null, null, false),
        (3, 1, 3, 'RSI', 'EDUC', 1, 123, false, now()::date - 1, '09:00', '10:30', (now()::date - 2)::timestamp, 'TEST.USER', null, null, null, false);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES  (1, 1, 'A1234BC', 1),
        (2, 1, 'B2345CD', 2),
        (3, 1, 'C3456DE', 3);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, attended, attendance_recorded_time, attendance_recorded_by)
VALUES  (4, 2, 'A1234BC', 1, null, null, null),
        (5, 2, 'B2345CD', 2, true, (now()::date - 1)::timestamp, 'ATTENDANCE.RECORDED.BY'),
        (6, 2, 'C3456DE', 3, true, (now()::date - 1)::timestamp, 'ATTENDANCE.RECORDED.BY'),
        (7, 2, 'D4567EF', 4, true, (now()::date - 1)::timestamp, 'ATTENDANCE.RECORDED.BY'),
        (8, 2, 'E5678FG', 5, false, (now()::date - 1)::timestamp, 'ATTENDANCE.RECORDED.BY'),
        (9, 2, 'F6789GH', 6, false, (now()::date - 1)::timestamp, 'ATTENDANCE.RECORDED.BY');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES  (10, 3, 'A1234BC', 1, null, null, null, now()::timestamp, 1, 'REMOVED.BY', true), -- Permanent removal by user
        (11, 3, 'B2345CD', 2, true, (now()::date - 1)::timestamp, 'ATTENDANCE.RECORDED.BY', now()::timestamp, 2, 'REMOVED.BY', false), -- Temporary removal by user after being marked as attended
        (12, 3, 'C3456DE', 3, false, (now()::date - 1)::timestamp, 'ATTENDANCE.RECORDED.BY', now()::timestamp, 2, 'REMOVED.BY', false); -- Temporary removal by user after being marked as non-attended

-- =====================================================================================
-- Single appointments.
-- Appointment with id 4 starting yesterday has no attendance marked and was cancelled.
-- Appointment with id 5 starting yesterday has no attendance marked and was deleted.
-- Appointment with id 6 starting yesterday has no attendees.
-- Appointment with id 7 starting yesterday has one removed attendee.
-- Appointment with id 8 starting yesterday has one soft deleted attendee.
-- Appointment with id 9 starting today has no attendance marked.
-- Appointment with id 10 starting today has its attendee marked as attended.
-- Appointment with id 11 starting today has its attendee marked as non-attended.
-- =====================================================================================

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES  (2, 'INDIVIDUAL', 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '13:45', '14:15', 1, (now()::date - 1)::timestamp, 'TEST.USER'),
        (3, 'INDIVIDUAL', 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '14:15', '14:45', 1, (now()::date - 1)::timestamp, 'TEST.USER'),
        (4, 'INDIVIDUAL', 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '14:45', '15:15', 1, (now()::date - 1)::timestamp, 'TEST.USER'),
        (5, 'INDIVIDUAL', 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '14:45', '15:15', 1, (now()::date - 1)::timestamp, 'TEST.USER'),
        (6, 'INDIVIDUAL', 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '14:45', '15:15', 1, (now()::date - 1)::timestamp, 'TEST.USER'),
        (7, 'INDIVIDUAL', 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date, '13:45', '14:15', 1, (now()::date - 1)::timestamp, 'TEST.USER'),
        (8, 'INDIVIDUAL', 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date, '14:15', '14:45', 1, (now()::date - 1)::timestamp, 'TEST.USER'),
        (9, 'INDIVIDUAL', 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date, '14:45', '15:15', 1, (now()::date - 1)::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES  (4, 2, 1, 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '13:45', '14:15', (now()::date - 1)::timestamp, 'TEST.USER', (now()::date - 1)::timestamp, 2, 'CANCEL.USER', false),
        (5, 3, 1, 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '14:15', '14:45', (now()::date - 1)::timestamp, 'TEST.USER', now()::timestamp, 1, 'DELETE.USER', true),
        (6, 4, 1, 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '14:45', '15:15', (now()::date - 1)::timestamp, 'TEST.USER', null, null, null, false),
        (7, 5, 1, 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '14:45', '15:15', (now()::date - 1)::timestamp, 'TEST.USER', null, null, null, false),
        (8, 6, 1, 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date - 1, '14:45', '15:15', (now()::date - 1)::timestamp, 'TEST.USER', null, null, null, false),
        (9, 7, 1, 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date, '13:45', '14:15', (now()::date - 1)::timestamp, 'TEST.USER', null, null, null, false),
        (10, 8, 1, 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date, '14:15', '14:45', (now()::date - 1)::timestamp, 'TEST.USER', null, null, null, false),
        (11, 9, 1, 'RSI', 'CHAP', 'Jehovah''s Witness One to One', 1, 456, false, now()::date, '14:45', '15:15', (now()::date - 1)::timestamp, 'TEST.USER', null, null, null, false);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES  (13, 4, 'A1234BC', 1, null, null, null, null, null, null, false),
        (14, 5, 'B2345CD', 2, null, null, null, null, null, null, false),
        (15, 7, 'C3456DE', 3, null, null, null, now()::timestamp, 2, 'REMOVED.BY', false), -- Temporary removal by user
        (16, 8, 'C3456DE', 3, null, null, null, now()::timestamp, 1, 'REMOVED.BY', true), -- Prisoner status: Released
        (17, 9, 'A1234BC', 1, null, null, null, null, null, null, false),
        (18, 10, 'B2345CD', 2, true, (now()::date - 1)::timestamp, 'ATTENDANCE.RECORDED.BY', null, null, null, false),
        (19, 11, 'C3456DE', 3, false, (now()::date - 1)::timestamp, 'ATTENDANCE.RECORDED.BY', null, null, null, false);

-- =====================================================================================
-- Appointment set with three appointments starting today.
-- Appointment with id 12 starting today has has no attendance marked.
-- Appointment with id 13 starting today has its attendee marked as attended.
-- Appointment with id 14 starting today has its attendee marked as non attended.
-- =====================================================================================

INSERT INTO appointment_set (appointment_set_id, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, created_time, created_by)
VALUES  (1, 'RSI', 'MEDO', 1, 789, false, now()::date, (now()::date - 1)::timestamp, 'TEST.USER');

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (10, 'INDIVIDUAL', 'RSI', 'MEDO', 1, 789, false, now()::date, '09:00', '09:15', (now()::date - 1)::timestamp, 'TEST.USER'),
        (11, 'INDIVIDUAL', 'RSI', 'MEDO', 1, 789, false, now()::date, '09:15', '09:30', (now()::date - 1)::timestamp, 'TEST.USER'),
        (12, 'INDIVIDUAL', 'RSI', 'MEDO', 1, 789, false, now()::date, '09:30', '09:45', (now()::date - 1)::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (12, 10, 1, 'RSI', 'MEDO', 1, 789, false, now()::date, '09:00', '09:15', (now()::date - 1)::timestamp, 'TEST.USER'),
        (13, 11, 1, 'RSI', 'MEDO', 1, 789, false, now()::date, '09:15', '09:30', (now()::date - 1)::timestamp, 'TEST.USER'),
        (14, 12, 1, 'RSI', 'MEDO', 1, 789, false, now()::date, '09:30', '09:45', (now()::date - 1)::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, attended, attendance_recorded_time, attendance_recorded_by)
VALUES  (20, 12, 'A1234BC', 1, null, null, null),
        (21, 13, 'B2345CD', 2, true, now()::timestamp, 'ATTENDANCE.RECORDED.BY'),
        (22, 14, 'C3456DE', 3, false, now()::timestamp, 'ATTENDANCE.RECORDED.BY');

INSERT INTO appointment_set_appointment_series (appointment_set_appointment_series_id, appointment_set_id, appointment_series_id)
VALUES  (1, 1, 10),
        (2, 1, 11),
        (3, 1, 12);
