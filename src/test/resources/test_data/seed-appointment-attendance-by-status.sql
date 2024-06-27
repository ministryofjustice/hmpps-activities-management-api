INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (1, 'DAILY', 3);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by, appointment_organiser_id)
VALUES (1, 'GROUP', 'RSI', 'EDUC', 1, 123, false, now()::date - 1, '09:00', '10:30', 1, (now()::date - 2)::timestamp, 'TEST.USER', 1);


INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, custom_name)
VALUES  (1, 1, 1, 'RSI', 'EDUC', 2, 123, false, now()::date - 1, '09:00', '10:30', (now()::date - 2)::timestamp, 'TEST.USER', (now()::date - 1)::timestamp, 2, 'CANCEL.USER', false, null),
        (2, 1, 3, 'RSI', 'EDUC', 1, 123, false, now()::date - 1, '09:00', '10:30', (now()::date - 2)::timestamp, 'TEST.USER', null, null, null, false, 'custom');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, attended)
VALUES  (1, 2, 'A1234BC', 1, null),
        (2, 2, 'B2345CD', 2, true),
        (4, 1, 'B2346CD', 4, null),
        (3, 2, 'C3456DE', 3, false);
