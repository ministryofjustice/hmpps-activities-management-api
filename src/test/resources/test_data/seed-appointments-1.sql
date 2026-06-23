INSERT INTO appointment_series (appointment_series_id, appointment_type, category_code, custom_name, appointment_tier_id, prison_code, dps_location_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (3, 'INDIVIDUAL', 'OIC', 'Appointment description', 1, 'MDI', '11111111-1111-1111-1111-111111111111', 1, false, '2022-10-01', '09:00', '10:30', 'Appointment series level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, appointment_tier_id, dps_location_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (4, 3, 1, 'MDI', 'OIC', 'Appointment description', 1, '11111111-1111-1111-1111-111111111111', 1, false, '2022-10-01', '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_series (appointment_series_id, appointment_type, category_code, custom_name, appointment_tier_id, prison_code, dps_location_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (4, 'INDIVIDUAL', 'OIC', 'Appointment description', 1, 'MDI', '11111111-1111-1111-1111-111111111111', 1, false, '2022-10-01', '09:00', '10:30', 'Appointment series level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, appointment_tier_id, dps_location_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (5, 4, 1, 'RSI', 'OIC', 'Appointment description', 1, '11111111-1111-1111-1111-111111111111', 1, false, '2022-10-01', '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (5, 4, 'A11111A', 1200993);
