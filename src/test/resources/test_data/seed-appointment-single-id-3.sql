INSERT INTO appointment_series (appointment_series_id, appointment_type, category_code, custom_name, appointment_tier_id, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (3, 'INDIVIDUAL', 'AC1', 'Appointment description', 1, 'MDI', 123, false, '2022-10-01', '09:00', '10:30', 'Appointment series level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (4, 3, 1, 'MDI', 'AC1', 'Appointment description', 1, 123, false, '2022-10-01', '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (5, 4, 'A11111A', 1200993);
