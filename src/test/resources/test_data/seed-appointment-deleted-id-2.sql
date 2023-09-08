INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (2, 'INDIVIDUAL', 'TPR', 'AC1', 4, 123, false, now()::date + 1, '09:00', '10:30', 'Appointment series level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES (3, 2, 1, 'TPR', 'AC1', 4, 123, false, now()::date + 1, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER', now()::timestamp, 1, 'TEST.USER', true);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (4, 3, 'A1234BC', 456);
