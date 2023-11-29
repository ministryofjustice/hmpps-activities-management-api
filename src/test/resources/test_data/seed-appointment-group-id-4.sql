INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (3, 'GROUP', 'MDI', 'AC1', 1, 123, false, '2022-10-01', '09:00', '10:30', 'Appointment series level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, extra_information, created_time, created_by)
VALUES (3, 3, 1, 'MDI', 'AC1', 1, 123, false, '2022-10-01', '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (4, 3, 'A5193DY', 1200993);

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (5, 3, 'G4793VF', 1200994);
