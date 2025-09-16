INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (2, 'WEEKLY', 4);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, dps_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, extra_information, created_time, created_by)
VALUES (7, 'GROUP', 'TPR', 'OIC', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date + 1, '09:00', '10:30', 2, 'Appointment series level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, dps_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by) VALUES
    (20, 7, 1, 'TPR', 'OIC', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date + 1, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER'),
    (21, 7, 2, 'TPR', 'OIC', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date + 8, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER'),
    (22, 7, 3, 'TPR', 'OIC', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date + 15, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER'),
    (23, 7, 4, 'TPR', 'OIC', 1, 123, '44444444-1111-2222-3333-444444444444', false, now()::date + 22, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id) VALUES
    (30, 20, 'A1234BC', 456),
    (31, 20, 'B2345CD', 457),
    (32, 20, 'C3456DE', 458),
    (33, 21, 'A1234BC', 456),
    (34, 21, 'B2345CD', 457),
    (35, 21, 'C3456DE', 458),
    (36, 22, 'A1234BC', 456),
    (37, 22, 'B2345CD', 457),
    (38, 22, 'C3456DE', 458),
    (39, 23, 'A1234BC', 456),
    (40, 23, 'B2345CD', 457),
    (41, 23, 'C3456DE', 458);

