INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (2, 'WEEKLY', 4);

INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, extra_information, created_time, created_by)
VALUES (7, 'GROUP', 'TPR', 'AC1', 1, 123, false, now()::date + 1, '09:00', '10:30', 2, 'Appointment series level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (20, 7, 1, 'TPR', 'AC1', 1, 123, false, now()::date + 1, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (21, 7, 2, 'TPR', 'AC1', 1, 123, false, now()::date + 8, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (22, 7, 3, 'TPR', 'AC1', 1, 123, false, now()::date + 15, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (23, 7, 4, 'TPR', 'AC1', 1, 123, false, now()::date + 22, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (30, 20, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (31, 20, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (32, 20, 'C3456DE', 458);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (33, 21, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (34, 21, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (35, 21, 'C3456DE', 458);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (36, 22, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (37, 22, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (38, 22, 'C3456DE', 458);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (39, 23, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (40, 23, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (41, 23, 'C3456DE', 458);

