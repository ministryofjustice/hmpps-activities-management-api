INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (6, 'INDIVIDUAL', 'TPR', 'AC1', 'Appointment description', 1, 123, false, now()::date + 1, '09:00', '09:15', 'Medical appointment for A1234BC', now()::timestamp, 'TEST.USER');
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (7, 'INDIVIDUAL', 'TPR', 'AC1', 'Appointment description', 1, 123, false, now()::date + 1, '09:15', '09:30', 'Medical appointment for B2345CD', now()::timestamp, 'TEST.USER');
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (8, 'INDIVIDUAL', 'TPR', 'AC1', 'Appointment description', 1, 123, false, now()::date + 1, '09:30', '09:45', 'Medical appointment for C3456DE', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (6, 6, 1, 'TPR', 'AC1', 'Appointment description', 1, 123, false, now()::date + 1, '09:00', '09:15', 'Medical appointment for A1234BC', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (7, 7, 1, 'TPR', 'AC1', 'Appointment description', 1, 123, false, now()::date + 1, '09:15', '09:30', 'Medical appointment for B2345CD', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (8, 8, 1, 'TPR', 'AC1', 'Appointment description', 1, 123, false, now()::date + 1, '09:30', '09:45', 'Medical appointment for C3456DE', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (6, 6, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (7, 7, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (8, 8, 'C3456DE', 458);

INSERT INTO appointment_set (appointment_set_id, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, created_time, created_by)
VALUES (6, 'TPR', 'AC1', 'Appointment description', 1, 123, false, now()::date + 1, now()::timestamp, 'TEST.USER');

INSERT INTO appointment_set_appointment_series (appointment_set_appointment_series_id, appointment_set_id, appointment_series_id)
VALUES (6, 6, 6);
INSERT INTO appointment_set_appointment_series (appointment_set_appointment_series_id, appointment_set_id, appointment_series_id)
VALUES (7, 6, 7);
INSERT INTO appointment_set_appointment_series (appointment_set_appointment_series_id, appointment_set_id, appointment_series_id)
VALUES (8, 6, 8);
