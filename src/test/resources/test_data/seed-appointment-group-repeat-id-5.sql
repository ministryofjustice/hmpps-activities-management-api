INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (1, 'WEEKLY', 4);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, dps_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, extra_information, created_time, created_by)
VALUES (5, 'GROUP', 'TPR', 'AC1', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date - 3, '09:00', '10:30', 1, 'Appointment series level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, dps_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by) VALUES
    (10, 5, 1, 'TPR', 'AC1', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date - 3, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER'),
    (11, 5, 2, 'TPR', 'AC1', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date + 4, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER'),
    (12, 5, 3, 'TPR', 'AC1', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date + 11, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER'),
    (13, 5, 4, 'TPR', 'AC1', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date + 18, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id) VALUES
    (20, 10, 'A1234BC', 456),
    (21, 10, 'B2345CD', 457),
    (22, 11, 'A1234BC', 456),
    (23, 11, 'B2345CD', 457),
    (24, 12, 'A1234BC', 456),
    (25, 12, 'B2345CD', 457),
    (26, 13, 'A1234BC', 456),
    (27, 13, 'B2345CD', 457);

