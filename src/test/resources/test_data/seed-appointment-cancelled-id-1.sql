INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by, cancelled_by, cancelled_time, cancellation_start_date, cancellation_start_time)
VALUES (2, 'INDIVIDUAL', 'TPR', 'AC1', 1, 123, false, now()::date + 1, '09:00', '10:30', 'Appointment series level comment', now()::timestamp, 'TEST.USER', 'DHOUSTON_GEN', '2024-05-16 10:59:08.841', now()::date, '11:30:00');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES (3, 2, 1, 'TPR', 'AC1', 1, 123, false, now()::date + 1, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER', now()::timestamp, 2, 'TEST.USER', false);

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES (4, 2, 1, 'TPR', 'AC1', 1, 123, false, now()::date + 2, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER', now()::timestamp, 2, 'TEST.USER', false);

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES (5, 2, 1, 'TPR', 'AC1', 1, 123, false, now()::date + 3, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER', now()::timestamp, 2, 'TEST.USER', false);

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES (6, 2, 1, 'TPR', 'AC1', 1, 123, false, now()::date - 6, now()::time - INTERVAL '60 Minutes', now()::time - INTERVAL '30 Minutes', 'Appointment level comment', now()::timestamp, 'TEST.USER', now()::timestamp, 2, 'TEST.USER', false);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (3, 3, 'A1234BC', 456);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (4, 4, 'A1234BC', 456);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (5, 5, 'A1234BC', 456);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (6, 6, 'A1234BC', 456);
