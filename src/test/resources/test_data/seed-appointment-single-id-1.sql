INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, custom_name, appointment_tier_id, appointment_organiser_id, internal_location_id, in_cell, dps_location_id, start_date, start_time, end_time, extra_information, prisoner_extra_information, created_time, created_by)
VALUES (1, 'INDIVIDUAL', 'TPR', 'OIC', 'Appointment description', 2, 1, 123, false, '44444444-1111-2222-3333-444444444444', now()::date + 1, '09:00', '10:30', 'Appointment series level comment', 'Prisoner series level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, appointment_tier_id, appointment_organiser_id, internal_location_id, in_cell, dps_location_id, start_date, start_time, end_time, extra_information, prisoner_extra_information, created_time, created_by)
VALUES (2, 1, 1, 'TPR', 'OIC', 'Appointment description', 2, 1, 123, false, '44444444-1111-2222-3333-444444444444', now()::date + 1, '09:00', '10:30', 'Appointment level comment', 'Prisoner level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (3, 2, 'A1234BC', 456);

